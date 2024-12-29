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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.io.File;

import androidx.annotation.NonNull;

class LabelPainter {
    private interface IDrawer {
        void draw(Canvas canvas);

        void drawShadow(Canvas canvas);

        void draw(Canvas canvas, Rect src, Rect dst);

        RectF getBounds();
    }

    private class InDrawer implements IDrawer {
        private float mSizeFactor;
        private float mX, mY;

        private InDrawer(float sizeFactor, float x, float y) {
            mSizeFactor = sizeFactor;
            setPosition(x, y);
            setPaintSettings(mSizeFactor);
        }

        @Override
        public void draw(Canvas canvas) {
            drawOutline(canvas, mX, mY);
            canvas.drawText(mLabel.getText(), mX, mY, mPaint);
        }

        @Override
        public void drawShadow(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            float rx = 10f;
            float ry = 10f;
            mPaint.setStrokeWidth(0f);

            mPaint.setColor(Color.LTGRAY);
            mPaint.setAlpha(100);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(bounds, rx, ry, mPaint);

            mPaint.setAlpha(255);
            mPaint.setStyle(Paint.Style.STROKE);

            mPaint.setColor(Color.BLUE);
            canvas.drawRoundRect(bounds, rx, ry, mPaint);

            mPaint.setColor(Color.GREEN);
            bounds.inset(-1f, -1f);
            canvas.drawRoundRect(bounds, rx, ry, mPaint);

            mPaint.setColor(Color.RED);
            bounds.inset(-1f, -1f);
            canvas.drawRoundRect(bounds, rx, ry, mPaint);

            setPaintSettings(mSizeFactor);
        }

        @Override
        public void draw(Canvas canvas, Rect src, Rect dst) {
            float factor = (dst.height() / (float) src.height());
            float x = (mX - src.left) * factor;
            float y = (mY - src.top) * factor;
            setSizePaintSettings(factor * mSizeFactor);
            drawOutline(canvas, x, y);
            canvas.drawText(mLabel.getText(), x, y, mPaint);
            setSizePaintSettings(mSizeFactor);
        }

        @Override
        public RectF getBounds() {
            RectF bounds = new RectF(getTextBounds());
            bounds.offset(mX, mY);
            if (mLabel.getOutline()) {
                float inset = mLabel.getOutlineSize() * mPaint.getTextSize();
                bounds.inset(-inset, -inset);
            }
            return bounds;
        }

        private void setPosition(float x, float y) {
            mX = x;
            mY = y;
        }

        private float getOneLetterSize() {
            Rect bounds = new Rect();
            mPaint.getTextBounds("M", 0, 1, bounds);
            return bounds.width();
        }

        private Rect getTextBounds() {
            Rect bounds = new Rect();
            String text = mLabel.getText();
            mPaint.getTextBounds(text, 0, text.length(), bounds);
            return bounds;
        }

        private void drawOutline(Canvas canvas, float x, float y) {
            if (mLabel.getOutline()) {
                setOutlinePaintSettings();
                canvas.drawText(mLabel.getText(), x, y, mPaint);
                setTextPaintSettings();
            }
        }

        private void setPaintSettings(float sizeFactor) {
            mPaint.setAlpha(255);
            try {
                Typeface tf = Typeface.create(
                        createTypefaceFromFontFile(),
                        createTypefaceFromFontAttributes());
                mPaint.setTypeface(tf);
            } catch (Exception ignore) {
            }
            setTextPaintSettings();
            setSizePaintSettings(sizeFactor);
        }

        private void setOutlinePaintSettings() {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mLabel.getOutlineColor());
        }

        private void setTextPaintSettings() {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mLabel.getForeColor());
        }

        private void setSizePaintSettings(float sizeFactor) {
            float textSize = mLabel.getTextSize() * sizeFactor;
            mPaint.setTextSize(textSize);
            mPaint.setStrokeWidth(mLabel.getOutlineSize() * textSize);
        }

        private Typeface createTypefaceFromFontFile() {
            Typeface typeface = null; // Typeface.DEFAULT
            String fontFilePath = mLabel.getFamilyName();

            if (!fontFilePath.equalsIgnoreCase(Label.DEFAULT_FONT)) {
                File fontFile = new File(fontFilePath);
                if (fontFile.exists() && fontFile.canRead())
                    typeface = Typeface.createFromFile(fontFilePath);
            }
            return typeface;
        }

        private int createTypefaceFromFontAttributes() {
            int typeface = Typeface.NORMAL;

            if (mLabel.getBold() && mLabel.getItalic())
                typeface = Typeface.BOLD_ITALIC;
            else {
                if (mLabel.getBold())
                    typeface = Typeface.BOLD;
                else if (mLabel.getItalic())
                    typeface = Typeface.ITALIC;
            }
            return typeface;
        }
    }

    private class OutDrawer implements IDrawer {
        private Path mPath;
        private RectF mBoundsOutside;
        private float mMinSize, mX, mY;

        private OutDrawer(float min) {
            mMinSize = min * 0.5f;
            mPaint.setAlpha(255);
            mPaint.setStrokeWidth(0f);
        }

        private void leftOut(RectF rect, float screenH) {
            mX = 0f;
            mY = Math.min(Math.max(mMinSize, rect.top + rect.height() * 0.5f), screenH - mMinSize);
            mPath = getLeftAlignedTriangle(mX, mY, mMinSize);
            mBoundsOutside = new RectF(mX, mY - mMinSize, mX + mMinSize, mY + mMinSize);
        }

        private void topOut(RectF rect, float screenW) {
            mX = Math.min(Math.max(mMinSize, rect.left + rect.width() * 0.5f), screenW - mMinSize);
            mY = 0f;
            mPath = getTopAlignedTriangle(mX, mY, mMinSize);
            mBoundsOutside = new RectF(mX - mMinSize, mY, mX + mMinSize * 0.5f, mY + mMinSize);
        }

        private void rightOut(RectF rect, float screenW, float screenH) {
            mX = screenW;
            mY = Math.min(Math.max(mMinSize, rect.top + rect.height() * 0.5f), screenH - mMinSize);
            mPath = getRightAlignedTriangle(mX, mY, mMinSize);
            mBoundsOutside = new RectF(mX - mMinSize, mY - mMinSize, mX, mY + mMinSize);
        }

        private void bottomOut(RectF rect, float screenW, float screenH) {
            mX = Math.min(Math.max(mMinSize, rect.left + rect.width() * 0.5f), screenW - mMinSize);
            mY = screenH;
            mPath = getBottomAlignedTriangle(mX, mY, mMinSize);
            mBoundsOutside = new RectF(mX - mMinSize, mY - mMinSize, mX + mMinSize, mY);
        }

        private Path getLeftAlignedTriangle(float x, float y, float r) {
            Path path = new Path();
            path.moveTo(x, y - r);
            path.lineTo(x, y + r);
            path.lineTo(x + r * 0.6f, y);
            path.lineTo(x, y - r);
            return path;
        }

        private Path getTopAlignedTriangle(float x, float y, float r) {
            Path path = new Path();
            path.moveTo(x - r, y);
            path.lineTo(x, y + r * 0.6f);
            path.lineTo(x + r, y);
            path.lineTo(x - r, y);
            return path;
        }

        private Path getRightAlignedTriangle(float x, float y, float r) {
            Path path = new Path();
            path.moveTo(x, y - r);
            path.lineTo(x - r * 0.6f, y);
            path.lineTo(x, y + r);
            path.lineTo(x, y - r);
            return path;
        }

        private Path getBottomAlignedTriangle(float x, float y, float r) {
            Path path = new Path();
            path.moveTo(x - r, y);
            path.lineTo(x, y - r * 0.6f);
            path.lineTo(x + r, y);
            path.lineTo(x - r, y);
            return path;
        }

        @Override
        public void draw(Canvas canvas) {
            mPaint.setColor(mLabel.getForeColor());
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(mPath, mPaint);

            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(mPath, mPaint);
        }

        @Override
        public void draw(Canvas canvas, Rect src, Rect dst) {
        }

        @Override
        public void drawShadow(Canvas canvas) {
            float r = 2f * mMinSize;

            mPaint.setColor(Color.LTGRAY);
            mPaint.setAlpha(100);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mX, mY, r, mPaint);

            mPaint.setAlpha(255);
            mPaint.setStyle(Paint.Style.STROKE);

            mPaint.setColor(Color.RED);
            canvas.drawCircle(mX, mY, r + 1f, mPaint);
            mPaint.setColor(Color.GREEN);
            canvas.drawCircle(mX, mY, r, mPaint);
            mPaint.setColor(Color.BLUE);
            canvas.drawCircle(mX, mY, r - 1f, mPaint);
        }

        @Override
        public RectF getBounds() {
            return mBoundsOutside;
        }
    }

    private final Paint mPaint;
    private Label mLabel;
    private IDrawer mDrawer;

    LabelPainter(@NonNull Label label) {
        mLabel = label;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    void draw(Canvas canvas) {
        mDrawer.draw(canvas);
    }

    void drawActive(Canvas canvas) {
        mDrawer.drawShadow(canvas);
        mDrawer.draw(canvas);
    }

    void draw(Canvas canvas, Rect src, Rect dst) {
        mDrawer.draw(canvas, src, dst);
    }

    RectF getBounds() {
        return mDrawer.getBounds();
    }

    void setLabel(@NonNull Label label) {
        mLabel = label;
    }

    void moveLabelInside(float sizeFactor, float screenW, float screenH, Position position) {
        if (isLabelInside())
            return;

        float x = position.getX();
        float y = position.getY();
        InDrawer inDrawer = new InDrawer(sizeFactor, x, y);
        RectF rect = inDrawer.getBounds();
        float min = Math.min(getMinSize(sizeFactor), inDrawer.getOneLetterSize());

        if (rect.right < min)  // left out
            x = min - rect.width();
        else if (rect.bottom < min) // top out
            y = min;
        else if (rect.left > (screenW - min))  // right out
            x = screenW - min;
        else if (rect.top > (screenH - min))  // bottom out
            y = screenH + rect.height() - min;

        inDrawer.setPosition(x, y);
        mDrawer = inDrawer;
        position.set(x, y);
    }

    void update(float sizeFactor, float screenW, float screenH, Position position) {
        InDrawer inDrawer = new InDrawer(sizeFactor, position.getX(), position.getY());
        RectF rect = inDrawer.getBounds();
        float minSize = getMinSize(sizeFactor);
        float min = Math.min(minSize, inDrawer.getOneLetterSize());

        OutDrawer outDrawer = null;
        if (rect.right < min) { // left out
            outDrawer = new OutDrawer(minSize);
            outDrawer.leftOut(rect, screenH);
        } else if (rect.bottom < min) {// top out
            outDrawer = new OutDrawer(minSize);
            outDrawer.topOut(rect, screenW);
        } else if (rect.left > (screenW - min)) { // right out
            outDrawer = new OutDrawer(minSize);
            outDrawer.rightOut(rect, screenW, screenH);
        } else if (rect.top > (screenH - min)) { // bottom out
            outDrawer = new OutDrawer(minSize);
            outDrawer.bottomOut(rect, screenW, screenH);
        }

        mDrawer = outDrawer == null ? inDrawer : outDrawer;
    }

    private boolean isLabelInside() {
        return mDrawer instanceof InDrawer;
    }

    private float getMinSize(float sizeFactor) {
        return 1.5f * sizeFactor;
    }
}
