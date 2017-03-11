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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Label implements Serializable {
    public static final float TEXT_SIZE_NORMAL = 2f;
    public static final float OUTLINE_SIZE_NORMAL = 0.05f;
    private String mText;
    private float mTextSize, mOutlineSize;
    private String mFamilyName;
    private boolean mBold, mItalic, mOutline;
    private int mForeColor, mBackColor, mOutlineColor;

    public Label() {
        mText = "";
        mTextSize = TEXT_SIZE_NORMAL;
        mFamilyName = null;
        mBold = true;
        mItalic = false;
        mForeColor = Color.BLACK;
        mBackColor = Color.TRANSPARENT;
        mOutline = true;
        mOutlineSize = OUTLINE_SIZE_NORMAL;
        mOutlineColor = Color.WHITE;
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

    public void setTextSize(float size) {
        if (size > 0f)
            mTextSize = size;
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

    public boolean getOutline() {
        return mOutline;
    }

    public void setOutline(boolean outline) {
        mOutline = outline;
    }

    public float getOutlineSize() {
        return mOutlineSize;
    }

    public void setOutlineSize(float size) {
        mOutlineSize = size;
    }

    public int getOutlineColor() {
        return mOutlineColor;
    }

    public void setOutlineColor(int color) {
        mOutlineColor = color;
    }

    public Label getClone() {
        Label clone = new Label();
        try {
            for (Field field : getClass().getDeclaredFields()) {
                if (!Modifier.isFinal(field.getModifiers()))
                    field.set(clone, field.get(this));
            }
        } catch (Exception ignore) {
        }
        return clone;
    }
}
