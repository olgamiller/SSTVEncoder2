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
package om.sstvencoder.TextOverlay;

import android.graphics.Color;

import java.io.Serializable;

public class Label implements Serializable {
    private final static int mVersion = 1;
    private String mText;
    private float mTextSize;
    private String mFamilyName;
    private boolean mBold, mItalic;
    private int mForeColor, mBackColor;

    public Label() {
        mText = "";
        mTextSize = 2.0f;
        mFamilyName = null;
        mBold = true;
        mItalic = false;
        mForeColor = Color.BLACK;
        mBackColor = Color.TRANSPARENT;
    }

    public int getVersion() {
        return mVersion;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        if (text != null)
            mText = text;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float textSize) {
        if (textSize > 0f)
            mTextSize = textSize;
    }

    public String getFamilyName() {
        return mFamilyName;
    }

    public void setFamilyName(String familyName) {
        mFamilyName = familyName;
    }

    public boolean getBold() {
        return mBold;
    }

    public void setBold(boolean bold) {
        mBold = bold;
    }

    public boolean getItalic() {
        return mItalic;
    }

    public void setItalic(boolean italic) {
        mItalic = italic;
    }

    public int getForeColor() {
        return mForeColor;
    }

    public void setForeColor(int color) {
        mForeColor = color;
    }

    public int getBackColor() {
        return mBackColor;
    }

    public void setBackColor(int color) {
        mBackColor = color;
    }
}
