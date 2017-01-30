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

class LabelContainer {
    private Label mLabel;
    private LabelPainter mPainter;
    private Position mPosition; // left-bottom corner

    LabelContainer(@NonNull Label label) {
        mLabel = label;
        mPainter = new LabelPainter(label);
        mPosition = new Position();
    }

    boolean contains(float x, float y) {
        return mPainter.getBounds().contains(x, y);
    }

    void draw(Canvas canvas) {
        mPainter.draw(canvas);
    }

    void drawActive(Canvas canvas) {
        mPainter.drawActive(canvas);
    }

    void draw(Canvas canvas, Rect src, Rect dst) {
        mPainter.draw(canvas, src, dst);
    }

    void jumpInside(float textSizeFactor, float screenW, float screenH) {
        mPainter.moveLabelInside(textSizeFactor, screenW, screenH, mPosition);
    }

    void offset(float x, float y) {
        mPosition.offset(x, y);
    }

    void update(float textSizeFactor, float screenW, float screenH) {
        mPainter.update(textSizeFactor, screenW, screenH, mPosition);
    }

    Label getContent() {
        return mLabel;
    }

    void setContent(@NonNull Label label) {
        mLabel = label;
        mPainter.setLabel(label);
    }

    void write(IWriter writer) throws IOException {
        writer.beginRootObject();
        {
            writer.write("position_x", mPosition.getX());
            writer.write("position_y", mPosition.getY());
            writer.beginObject("label");
            {
                writeLabel(writer, mLabel);
            }
            writer.endObject();
        }
        writer.endObject();
    }

    void read(IReader reader) throws IOException {
        reader.beginRootObject();
        {
            mPosition.set(reader.readFloat(), reader.readFloat());
            reader.beginObject();
            {
                readLabel(reader, mLabel);
            }
            reader.endObject();
        }
        reader.endObject();
    }

    private void writeLabel(IWriter writer, Label label) throws IOException {
        writer.write("version", label.getVersion());
        writer.write("text", label.getText());
        writer.write("text_size", label.getTextSize());
        writer.write("family_name", label.getFamilyName());
        writer.write("bold", label.getBold());
        writer.write("italic", label.getItalic());
        writer.write("fore_color", label.getForeColor());
        writer.write("back_color", label.getBackColor());
    }

    private void readLabel(IReader reader, Label label) throws IOException {
        reader.readInt();
        label.setText(reader.readString());
        label.setTextSize(reader.readFloat());
        label.setFamilyName(reader.readString());
        label.setBold(reader.readBoolean());
        label.setItalic(reader.readBoolean());
        label.setForeColor(reader.readInt());
        label.setBackColor(reader.readInt());
    }
}

