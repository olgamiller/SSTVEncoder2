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

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import om.sstvencoder.Utility;

public class LabelCollection {
    private class Size {
        private float mW, mH;

        Size(float w, float h) {
            mW = w;
            mH = h;
        }

        float width() {
            return mW;
        }

        float height() {
            return mH;
        }
    }

    private final List<LabelContainer> mLabels;
    private Size mScreenSize;
    private float mTextSizeFactor;
    private LabelContainer mActiveLabel, mEditLabel;
    private float mPreviousX, mPreviousY;

    public LabelCollection() {
        mLabels = new LinkedList<>();
        mPreviousX = 0f;
        mPreviousY = 0f;
    }

    public void update(float w, float h) {
        if (mScreenSize != null) {
            float x = (w - mScreenSize.width()) / 2f;
            float y = (h - mScreenSize.height()) / 2f;
            for (LabelContainer label : mLabels)
                label.offset(x, y);
        }
        mScreenSize = new Size(w, h);
        mTextSizeFactor = getTextSizeFactor(w, h);
        for (LabelContainer label : mLabels)
            label.update(mTextSizeFactor, w, h);
    }

    private float getTextSizeFactor(float w, float h) {
        Rect bounds = Utility.getEmbeddedRect((int) w, (int) h, 320, 240);
        return 0.1f * bounds.height();
    }

    public void draw(Canvas canvas) {
        for (LabelContainer label : mLabels)
            label.draw(canvas);
        if (mActiveLabel != null)
            mActiveLabel.drawActive(canvas);
    }

    public void draw(Canvas canvas, Rect src, Rect dst) {
        for (LabelContainer label : mLabels)
            label.draw(canvas, src, dst);
    }

    public boolean moveLabelBegin(float x, float y) {
        mActiveLabel = find(x, y);
        if (mActiveLabel == null)
            return false;
        mLabels.remove(mActiveLabel);
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

    public void moveLabel(float x, float y) {
        mActiveLabel.offset(x - mPreviousX, y - mPreviousY);
        mActiveLabel.update(mTextSizeFactor, mScreenSize.width(), mScreenSize.height());
        mPreviousX = x;
        mPreviousY = y;
    }

    public void moveLabelEnd() {
        mLabels.add(mActiveLabel);
        mActiveLabel = null;
        mPreviousX = 0f;
        mPreviousY = 0f;
    }

    public Label editLabelBegin(float x, float y) {
        mEditLabel = find(x, y);
        if (mEditLabel == null) {
            mEditLabel = new LabelContainer(new Label());
            mEditLabel.offset(x, y);
        }
        return mEditLabel.getContent();
    }

    public void editLabelEnd(Label label) {
        if (label != null) { // not canceled
            if ("".equals(label.getText().trim())) {
                if (mLabels.contains(mEditLabel))
                    mLabels.remove(mEditLabel);
            } else {
                if (!mLabels.contains(mEditLabel))
                    mLabels.add(mEditLabel);
                mEditLabel.setContent(label);
                mEditLabel.update(mTextSizeFactor, mScreenSize.width(), mScreenSize.height());
            }
        }
        mEditLabel = null;
    }

    private LabelContainer find(float x, float y) {
        for (LabelContainer label : mLabels) {
            if (label.contains(x, y))
                return label;
        }
        return null;
    }

    private void add(LabelContainer label) {
        if (mLabels.size() == 0)
            mLabels.add(label);
        else
            mLabels.add(0, label);
    }

    public void write(@NonNull IWriter writer) throws IOException {
        writer.beginRootObject();
        {
            writer.write("width", mScreenSize.width());
            writer.write("height", mScreenSize.height());
            writer.beginArray("labels");
            {
                for (LabelContainer label : mLabels)
                    label.write(writer);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    public void read(@NonNull IReader reader) throws IOException {
        reader.beginRootObject();
        {
            float w = reader.readFloat();
            float h = reader.readFloat();
            reader.beginArray();
            {
                while (reader.hasNext()) {
                    LabelContainer label = new LabelContainer(new Label());
                    label.read(reader);
                    add(label);
                }
            }
            reader.endArray();
            update(w, h);
        }
        reader.endObject();
    }
}
