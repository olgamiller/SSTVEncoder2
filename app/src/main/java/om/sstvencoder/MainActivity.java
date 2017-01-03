/*
Copyright 2017 Olga Miller <olga.rgb@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package om.sstvencoder;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;

import om.sstvencoder.ModeInterfaces.IModeInfo;
import om.sstvencoder.TextOverlay.Label;

public class MainActivity extends AppCompatActivity {
    private static final String CLASS_NAME = "ClassName";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private Settings mSettings;
    private TextOverlayTemplate mTextOverlayTemplate;
    private CropView mCropView;
    private Encoder mEncoder;
    private File mFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCropView = (CropView) findViewById(R.id.cropView);
        mEncoder = new Encoder();
        IModeInfo mode = mEncoder.getModeInfo();
        mCropView.setModeSize(mode.getModeSize());
        setTitle(mode.getModeName());
        mSettings = new Settings(this);
        mSettings.load();
        mTextOverlayTemplate = new TextOverlayTemplate();
        mTextOverlayTemplate.load(mCropView.getLabels(), mSettings.getTextOverlayFile());
        loadImage(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        loadImage(intent);
        super.onNewIntent(intent);
    }

    private void loadImage(Intent intent) {
        Uri uri = getImageUriFromIntent(intent);
        boolean verbose = true;
        if (uri == null) {
            uri = mSettings.getImageUri();
            verbose = false;
        }
        if (loadImage(uri, verbose))
            mSettings.setImageUri(uri);
    }

    private Uri getImageUriFromIntent(Intent intent) {
        Uri uri = null;
        if (isIntentTypeValid(intent.getType()) && isIntentActionValid(intent.getAction())) {
            uri = intent.hasExtra(Intent.EXTRA_STREAM) ?
                    (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM) : intent.getData();
            if (uri == null) {
                String s = getString(R.string.load_img_err_txt_unsupported);
                showErrorMessage(getString(R.string.load_img_err_title), s, s + "\n\n" + intent);
            }
        }
        return uri;
    }

    // Set verbose to false for any Uri that might have expired (e.g. shared from browser).
    private boolean loadImage(Uri uri, boolean verbose) {
        if (uri != null) {
            try {
                ContentResolver resolver = getContentResolver();
                mCropView.setBitmapStream(resolver.openInputStream(uri));
                mCropView.rotateImage(getOrientation(resolver, uri));
                return true;
            } catch (FileNotFoundException ex) {
                if (ex.getCause() instanceof ErrnoException
                        && ((ErrnoException) ex.getCause()).errno == OsConstants.EACCES) {
                    requestPermissions();
                } else if (verbose) {
                    String s = getString(R.string.load_img_err_title) + ": \n" + ex.getMessage();
                    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
                }
            } catch (Exception ex) {
                if (verbose) {
                    String s = Utility.createMessage(ex) + "\n\n" + uri;
                    showErrorMessage(getString(R.string.load_img_err_title), ex.getMessage(), s);
                }
            }
        }
        mCropView.setNoBitmap();
        return false;
    }

    private boolean isIntentActionValid(String action) {
        return Intent.ACTION_SEND.equals(action);
    }

    private boolean isIntentTypeValid(String type) {
        return type != null && type.startsWith("image/");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void requestPermissions() {
        if (Build.VERSION_CODES.JELLY_BEAN > Build.VERSION.SDK_INT)
            return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    private void showErrorMessage(final String title, final String shortText, final String longText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(shortText);
        builder.setNeutralButton(getString(R.string.btn_ok), null);
        builder.setPositiveButton(getString(R.string.btn_send_email), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String device = Build.MANUFACTURER + ", " + Build.BRAND + ", " + Build.MODEL + ", " + Build.VERSION.RELEASE;
                String text = longText + "\n\n" + BuildConfig.VERSION_NAME + ", " + device;
                Intent intent = Utility.createEmailIntent(getString(R.string.email_subject), text);
                startActivity(Intent.createChooser(intent, getString(R.string.chooser_title)));
            }
        });
        builder.show();
    }

    private void showOrientationErrorMessage(Uri uri, Exception ex) {
        String title = getString(R.string.load_img_orientation_err_title);
        String longText = title + "\n\n" + Utility.createMessage(ex) + "\n\n" + uri;
        showErrorMessage(title, ex.getMessage(), longText);
    }

    public int getOrientation(ContentResolver resolver, Uri uri) {
        int orientation = 0;
        try {
            Cursor cursor = resolver.query(uri, new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
            if (cursor.moveToFirst())
                orientation = cursor.getInt(0);
            cursor.close();
        } catch (Exception ignore) {
            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                orientation = Utility.convertToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0));
            } catch (Exception ex) {
                showOrientationErrorMessage(uri, ex);
            }
        }
        return orientation;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        createModesMenu(menu);
        return true;
    }

    private void createModesMenu(Menu menu) {
        SubMenu modesSubMenu = menu.findItem(R.id.action_modes).getSubMenu();
        IModeInfo[] modeInfoList = mEncoder.getModeInfoList();
        for (IModeInfo modeInfo : modeInfoList) {
            MenuItem item = modesSubMenu.add(modeInfo.getModeName());
            Intent intent = new Intent();
            intent.putExtra(CLASS_NAME, modeInfo.getModeClassName());
            item.setIntent(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pick_picture:
                dispatchPickPictureIntent();
                return true;
            case R.id.action_take_picture:
                dispatchTakePictureIntent();
                return true;
            case R.id.action_save_wave:
                save();
                return true;
            case R.id.action_play:
                play();
                return true;
            case R.id.action_stop:
                mEncoder.stop();
                return true;
            case R.id.action_rotate:
                mCropView.rotateImage(90);
                return true;
            case R.id.action_modes:
                return true;
            default:
                String className = item.getIntent().getStringExtra(CLASS_NAME);
                if (mEncoder.setMode(className)) {
                    IModeInfo modeInfo = mEncoder.getModeInfo();
                    mCropView.setModeSize(modeInfo.getModeSize());
                    setTitle(modeInfo.getModeName());
                }
                return true;
        }
    }

    public void startEditTextActivity(@NonNull Label label) {
        Intent intent = new Intent(this, EditTextActivity.class);
        intent.putExtra(EditTextActivity.EXTRA, label);
        startActivityForResult(intent, EditTextActivity.REQUEST_CODE);
    }

    private void dispatchTakePictureIntent() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Device has no camera.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            mFile = Utility.createImageFilePath();
            if (mFile != null) {
                Uri uri = FileProvider.getUriForFile(this, "om.sstvencoder", mFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchPickPictureIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EditTextActivity.REQUEST_CODE:
                Label label = null;
                if (resultCode == RESULT_OK && data != null)
                    label = (Label) data.getSerializableExtra(EditTextActivity.EXTRA);
                mCropView.editLabelEnd(label);
                break;
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Uri uri = Uri.fromFile(mFile);
                    if (loadImage(uri, true)) {
                        mSettings.setImageUri(uri);
                        addImageToGallery(uri);
                    }
                }
                break;
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (loadImage(uri, true))
                        mSettings.setImageUri(uri);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void addImageToGallery(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        sendBroadcast(intent);
    }

    private void play() {
        mEncoder.play(mCropView.getBitmap());
    }

    private void save() {
        File file = Utility.createWaveFilePath();
        if (mEncoder.save(mCropView.getBitmap(), file)) {
            ContentValues values = Utility.getWavContentValues(file);
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
            getContentResolver().insert(uri, values);
            Toast.makeText(this, file.getName() + " saved.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSettings.save();
        mTextOverlayTemplate.save(mCropView.getLabels(), mSettings.getTextOverlayFile());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEncoder.destroy();
    }
}