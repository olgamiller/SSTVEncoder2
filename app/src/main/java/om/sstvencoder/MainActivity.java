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
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

import om.sstvencoder.ModeInterfaces.IModeInfo;
import om.sstvencoder.Output.WaveFileOutputContext;
import om.sstvencoder.TextOverlay.Label;

public class MainActivity extends AppCompatActivity {
    private static final String CLASS_NAME = "ClassName";
    private static final int REQUEST_LOAD_IMAGE_PERMISSION = 1;
    private static final int REQUEST_SAVE_WAVE_PERMISSION = 2;
    private static final int REQUEST_IMAGE_CAPTURE_PERMISSION = 3;
    private static final int REQUEST_PICK_IMAGE = 11;
    private static final int REQUEST_IMAGE_CAPTURE = 12;
    private Settings mSettings;
    private TextOverlayTemplate mTextOverlayTemplate;
    private CropView mCropView;
    private Encoder mEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCropView = findViewById(R.id.cropView);
        mEncoder = new Encoder(new MainActivityMessenger(this), getProgressBar(), getProgressBar2());

        mSettings = new Settings(this);
        mSettings.load();

        mTextOverlayTemplate = new TextOverlayTemplate();
        mTextOverlayTemplate.load(mCropView.getLabels(), mSettings.getTextOverlayFile());

        setMode(mSettings.getModeClassName());
        loadImage(getIntent());
    }

    private ProgressBarWrapper getProgressBar() {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView progressBarText = findViewById(R.id.progressBarText);
        return new ProgressBarWrapper(progressBar, progressBarText);
    }

    private ProgressBarWrapper getProgressBar2() {
        ProgressBar progressBar = findViewById(R.id.progressBar2);
        TextView progressBarText = findViewById(R.id.progressBarText2);
        return new ProgressBarWrapper(progressBar, progressBarText);
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
            // SecurityException in loadImage for Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            // uri = mSettings.getImageUri();
            verbose = false;
        }
        loadImage(uri, verbose);
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
        ContentResolver resolver = getContentResolver();
        InputStream stream = null;
        if (uri != null) {
            mSettings.setImageUri(uri);
            try {
                stream = resolver.openInputStream(uri);
            } catch (Exception ex) { // e.g. FileNotFoundException, SecurityException
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isPermissionException(ex)
                        && needsRequestReadPermission()) {
                    requestReadPermission(REQUEST_LOAD_IMAGE_PERMISSION);
                    return false;
                }
                showFileNotLoadedMessage(ex, verbose);
            }
        }
        if (stream == null || !loadImage(stream, resolver, uri)) {
            setDefaultBitmap();
            return false;
        }
        return true;
    }

    private boolean loadImage(InputStream stream, ContentResolver resolver, Uri uri) {
        try {
            mCropView.setBitmap(stream);
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (Exception ex) {
            String s = Utility.createMessage(ex) + "\n\n" + uri;
            showErrorMessage(getString(R.string.load_img_err_title), ex.getMessage(), s);
            return false;
        }
        mCropView.rotateImage(getOrientation(resolver, uri));
        return true;
    }

    private void setDefaultBitmap() {
        try {
            mCropView.setBitmap(getResources().openRawResource(R.raw.smpte_color_bars));
        } catch (Exception ignore) {
            mCropView.setNoBitmap();
        }
        mSettings.setImageUri(null);
    }

    private boolean isIntentActionValid(String action) {
        return Intent.ACTION_SEND.equals(action);
    }

    private boolean isIntentTypeValid(String type) {
        return type != null && type.startsWith("image/");
    }

    private boolean isPermissionException(Exception ex) {
        return ex.getCause() instanceof ErrnoException
                && ((ErrnoException) ex.getCause()).errno == OsConstants.EACCES;
    }

    private boolean needsRequestReadPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int state = ContextCompat.checkSelfPermission(this, permission);
        return state != PackageManager.PERMISSION_GRANTED;
    }

    private boolean needsRequestWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            return false;
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int state = ContextCompat.checkSelfPermission(this, permission);
        return state != PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadPermission(int requestCode) {
        String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    private void requestWritePermission(int requestCode) {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOAD_IMAGE_PERMISSION:
                if (permissionGranted(grantResults))
                    loadImage(mSettings.getImageUri(), false);
                else
                    setDefaultBitmap();
                break;
            case REQUEST_IMAGE_CAPTURE_PERMISSION:
                if (permissionGranted(grantResults))
                    dispatchTakePictureIntent();
                break;
            case REQUEST_SAVE_WAVE_PERMISSION:
                if (permissionGranted(grantResults))
                    save();
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean permissionGranted(@NonNull int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    private void showFileNotLoadedMessage(Exception ex, boolean verbose) {
        String s;
        if (verbose)
            s = getString(R.string.load_img_err_title) + ": \n" + ex.getMessage();
        else
            s = getString(R.string.message_prev_img_not_loaded);
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
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
            Cursor cursor = resolver.query(uri,
                    new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
                    null, null, null);
            if (cursor.moveToFirst())
                orientation = cursor.getInt(0);
            cursor.close();
        } catch (Exception ignore) {
            orientation = getExifOrientation(resolver, uri);
        }
        return orientation;
    }

    private int getExifOrientation(ContentResolver resolver, Uri uri) {
        int orientation = 0;
        InputStream in = null;
        try {
            in = resolver.openInputStream(uri);
            int orientationAttribute = (new ExifInterface(in)).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            orientation = Utility.convertToDegrees(orientationAttribute);
        } catch (Exception ex) {
            showOrientationErrorMessage(uri, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
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
        int id = item.getItemId();
        if (id == R.id.action_pick_picture) {
            dispatchPickPictureIntent();
        }
        else if (id == R.id.action_take_picture) {
            takePicture();
        }
        else if (id == R.id.action_save_wave) {
            if (needsRequestWritePermission())
                requestWritePermission(REQUEST_SAVE_WAVE_PERMISSION);
            else
                save();
        }
        else if (id == R.id.action_play) {
            play();
        }
        else if (id == R.id.action_stop) {
            mEncoder.stop();
        }
        else if (id == R.id.action_rotate) {
            mCropView.rotateImage(90);
        }
        else if (id == R.id.action_privacy_policy) {
            showTextPage(getString(R.string.action_privacy_policy), getString(R.string.action_privacy_policy_text));
        }
        else if (id == R.id.action_about) {
            showTextPage(getString(R.string.action_about), getString(R.string.action_about_text, BuildConfig.VERSION_NAME));
        }
        else if (id != R.id.action_modes) {
            String className = item.getIntent().getStringExtra(CLASS_NAME);
            setMode(className);
        }
        return true;
    }

    private void showTextPage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.show();
    }

    private void setMode(String modeClassName) {
        if (mEncoder.setMode(modeClassName)) {
            IModeInfo modeInfo = mEncoder.getModeInfo();
            mCropView.setModeSize(modeInfo.getModeSize());
            setTitle(modeInfo.getModeName());
            mSettings.setModeClassName(modeClassName);
        }
    }

    private void takePicture() {
        if (!hasCamera()) {
            Toast.makeText(this, getString(R.string.message_no_camera), Toast.LENGTH_LONG).show();
            return;
        }
        if (needsRequestWritePermission())
            requestWritePermission(REQUEST_IMAGE_CAPTURE_PERMISSION);
        else
            dispatchTakePictureIntent();
    }

    private boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    public void startEditTextActivity(@NonNull Label label) {
        Intent intent = new Intent(this, EditTextActivity.class);
        intent.putExtra(EditTextActivity.EXTRA, label);
        startActivityForResult(intent, EditTextActivity.REQUEST_CODE);
    }

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            Uri uri = Utility.createImageUri(this);
            if (uri != null) {
                mSettings.setImageUri(uri);
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
                    Uri uri = mSettings.getImageUri();
                    if (loadImage(uri, true))
                        addImageToGallery(uri);
                }
                break;
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK && data != null)
                    loadImage(data.getData(), true);
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
        if (Utility.isExternalStorageWritable()) {
            WaveFileOutputContext context
                    = new WaveFileOutputContext(getContentResolver(), Utility.createWaveFileName());
            mEncoder.save(mCropView.getBitmap(), context);
        }
    }

    public void completeSaving(WaveFileOutputContext context) {
        context.update();
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