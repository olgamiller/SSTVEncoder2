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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import om.sstvencoder.TextOverlay.Label;

public class EditTextActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, ColorFragment.OnColorSelectedListener {

    private enum EditColorMode {
        None,
        Text,
        Outline
    }

    public static final int REQUEST_CODE = 101;
    public static final String EXTRA = "EDIT_TEXT_EXTRA";
    private Label mLabel;
    private EditColorMode mEditColor;
    private List<String> mFontFilePathList;
    private CheckBox mEditItalic, mEditBold, mEditOutline;
    private int mClearTextButtonWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text);
        mEditColor = EditColorMode.None;
        mEditBold = findViewById(R.id.edit_bold);
        mEditItalic = findViewById(R.id.edit_italic);
        mEditOutline = findViewById(R.id.edit_outline);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLabel = ((Label) getIntent().getSerializableExtra(EXTRA)).getClone();
        initText();
        initTextSizeSpinner(mLabel.getTextSize());
        mEditBold.setChecked(mLabel.getBold());
        mEditItalic.setChecked(mLabel.getItalic());
        initFontFamilySpinner(mLabel.getFamilyName());
        mEditOutline.setChecked(mLabel.getOutline());
        initOutlineSizeSpinner(mLabel.getOutlineSize());
        findViewById(R.id.edit_color).setBackgroundColor(mLabel.getForeColor());
        findViewById(R.id.edit_outline_color).setBackgroundColor(mLabel.getOutlineColor());
        enableOutline(mEditOutline.isChecked());
    }

    private void initText() {
        EditText editText = findViewById(R.id.edit_text);
        int clearTextIcon = android.R.drawable.ic_menu_close_clear_cancel;
        Drawable drawable = ContextCompat.getDrawable(this, clearTextIcon);
        editText.setText(mLabel.getText());
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
        mClearTextButtonWidth = 2 * drawable.getIntrinsicWidth();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mLabel.setText(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        editText.setOnTouchListener(new View.OnTouchListener() {
            private boolean mClear;

            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (HitClearTextButton(view, e)) {
                            mClear = true;
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!HitClearTextButton(view, e))
                            mClear = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (HitClearTextButton(view, e) && mClear) {
                            ((EditText) view).setText("");
                            return true;
                        }
                        mClear = false;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        mClear = false;
                        break;
                }
                return false;
            }

            private boolean HitClearTextButton(View view, MotionEvent e) {
                int left = view.getRight() - mClearTextButtonWidth;
                return left < e.getX();
            }
        });
    }

    private void initFontFamilySpinner(String familyName) {
        Spinner spinner = findViewById(R.id.edit_font_family);
        spinner.setOnItemSelectedListener(this);
        mFontFilePathList = Utility.getSystemFontFilePaths();
        mFontFilePathList.add(0, Label.DEFAULT_FONT);
        List<String> fontFamilyNameList = getFontNames(mFontFilePathList);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, fontFamilyNameList));
        spinner.setSelection(mFontFilePathList.indexOf(familyName));
    }

    private static List<String> getFontNames(List<String> fontFilePathList) {
        List<String> fontNameList = new ArrayList<>();
        for (String path : fontFilePathList) {
            fontNameList.add(Utility.getFileNameWithoutExtension(path));
        }
        return fontNameList;
    }

    private void initTextSizeSpinner(float textSize) {
        Spinner spinner = findViewById(R.id.edit_text_size);
        spinner.setOnItemSelectedListener(this);
        String[] sizeList = new String[]
                {
                        getString(R.string.font_size_small),
                        getString(R.string.font_size_normal),
                        getString(R.string.font_size_large),
                        getString(R.string.font_size_huge)
                };
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sizeList));
        spinner.setSelection(textSizeToPosition(textSize));
    }

    private void initOutlineSizeSpinner(float outlineSize) {
        Spinner spinner = findViewById(R.id.edit_outline_size);
        spinner.setOnItemSelectedListener(this);
        String[] sizeList = new String[]
                {
                        getString(R.string.outline_size_thin),
                        getString(R.string.outline_size_normal),
                        getString(R.string.outline_size_thick)
                };
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sizeList));
        spinner.setSelection(outlineSizeToPosition(outlineSize));
    }

    private int textSizeToPosition(float textSize) {
        int position = (int) (textSize - 1f);
        if (0 <= position && position <= 3)
            return position;
        mLabel.setTextSize(Label.TEXT_SIZE_NORMAL);
        return 1;
    }

    private float positionToTextSize(int position) {
        return position + 1f;
    }

    private int outlineSizeToPosition(float outlineSize) {
        int position = (int) (outlineSize * 2f / Label.OUTLINE_SIZE_NORMAL - 1f);
        if (0 <= position && position <= 2)
            return position;
        mLabel.setOutlineSize(Label.OUTLINE_SIZE_NORMAL);
        return 1;
    }

    private float positionToOutlineSize(int position) {
        return Label.OUTLINE_SIZE_NORMAL * 0.5f * (position + 1f);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int parentId = parent.getId();
        if (parentId == R.id.edit_text_size) {
            mLabel.setTextSize(positionToTextSize(position));
        }
        else if (parentId == R.id.edit_outline_size) {
            mLabel.setOutlineSize(positionToOutlineSize(position));
        }
        else if (parentId == R.id.edit_font_family) {
            mLabel.setFamilyName(mFontFilePathList.get(position));
        }
    }

    private void enableOutline(boolean enabled) {
        findViewById(R.id.text_outline_size).setEnabled(enabled);
        findViewById(R.id.edit_outline_size).setEnabled(enabled);
        findViewById(R.id.text_outline_color).setEnabled(enabled);
        findViewById(R.id.edit_outline_color).setEnabled(enabled);
        @ColorInt
        int color = enabled ? mLabel.getOutlineColor() : Color.DKGRAY;
        findViewById(R.id.edit_outline_color).setBackgroundColor(color);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_text, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            done();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onBoldClick(View view) {
        mLabel.setBold(mEditBold.isChecked());
    }

    public void onItalicClick(View view) {
        mLabel.setItalic(mEditItalic.isChecked());
    }

    public void onOutlineClick(View view) {
        if (view.getId() == R.id.text_outline)
            mEditOutline.setChecked(!mEditOutline.isChecked());
        boolean outline = mEditOutline.isChecked();
        mLabel.setOutline(outline);
        enableOutline(outline);
    }

    public void onColorClick(View view) {
        showColorDialog(R.string.color, mLabel.getForeColor());
        mEditColor = EditColorMode.Text;
    }

    public void onOutlineColorClick(View view) {
        if (mEditOutline.isChecked()) {
            showColorDialog(R.string.outline_color, mLabel.getOutlineColor());
            mEditColor = EditColorMode.Outline;
        }
    }

    private void showColorDialog(int title, int color) {
        ColorFragment fragment = new ColorFragment();
        fragment.setTitle(title);
        fragment.setColor(color);
        fragment.addOnColorSelectedListener(this);
        fragment.show(getSupportFragmentManager(), ColorFragment.class.getName());
    }

    @Override
    public void onColorSelected(DialogFragment fragment, int color) {
        switch (mEditColor) {
            case Text:
                mLabel.setForeColor(color);
                findViewById(R.id.edit_color).setBackgroundColor(color);
                break;
            case Outline:
                mLabel.setOutlineColor(color);
                findViewById(R.id.edit_outline_color).setBackgroundColor(color);
                break;
        }
        mEditColor = EditColorMode.None;
    }

    @Override
    public void onCancel(DialogFragment fragment) {
        mEditColor = EditColorMode.None;
    }

    private void done() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA, mLabel);
        setResult(RESULT_OK, intent);
        finish();
    }
}
