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
package om.sstvencoder.ColorPalette;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

class GridColorPalette implements IColorPalette {

    static int[] getStandardColors() {
        return new int[]{
                Color.BLACK,
                Color.GRAY,
                Color.LTGRAY,
                Color.WHITE,
                Color.YELLOW,
                Color.CYAN,
                Color.GREEN,
                Color.MAGENTA,
                Color.RED,
                Color.BLUE
        };
    }

    private final static float STROKE_WIDTH_FACTOR = 6f;
    private final static float BOX_SIZE_DP = 96f;
    private final static float SPACE_FACTOR = 6f;
    private final int[] mColorList;
    private final Paint mPaint;
    private final RectF mSelectedBounds;
    private final float mDisplayMetricsDensity;
    private int mColumns, mRows;
    private float mWidth, mHeight;
    private float mBoxSize, mSpace, mStrokeWidth, mCornerRadius;
    private int mSelectedColorIndex;
    private boolean mValid;

    GridColorPalette(int[] colorList, float displayMetricsDensity) {
        final float CORNER_RADIUS = 3f;
        mColorList = colorList;
        mDisplayMetricsDensity = displayMetricsDensity;
        mCornerRadius = CORNER_RADIUS * mDisplayMetricsDensity;
        mPaint = new Paint();
        setPaintStyleForBox();
        mSelectedBounds = new RectF();
        mSelectedColorIndex = 0;
        mValid = false;
    }

    @Override
    public void updateSize(float width, float height) {
        mValid = width > 0 && height > 0;

        if (mValid && (mWidth != width || mHeight != height)) {
            mWidth = width;
            mHeight = height;
            updateGrid();
            mStrokeWidth = mSpace / STROKE_WIDTH_FACTOR;
            setSelectedColor(mSelectedColorIndex);
        }
    }

    // The approximately same box size independently on resolution has the higher priority.
    // Thus the possible filling of the last row is not supported here.
    private void updateGrid() {
        int boxes = mColorList.length;
        mBoxSize = BOX_SIZE_DP * mDisplayMetricsDensity;
        mSpace = mBoxSize / SPACE_FACTOR;

        mColumns = min((int) ((mWidth - mSpace) / (mBoxSize + mSpace) + 0.5f), boxes);
        mRows = (boxes + mColumns - 1) / mColumns; // ceil
        updateBoxSizeAndSpace();

        while (mRows * (mBoxSize + mSpace) + mSpace > mHeight) {
            ++mColumns;
            mRows = (boxes + mColumns - 1) / mColumns;
            updateBoxSizeAndSpace();
        }
    }

    private int min(int a, int b) {
        return a <= b ? a : b;
    }

    // Fill out the whole width of the View.
    private void updateBoxSizeAndSpace() {
        // Set 'space = boxSize / spaceFactor' into
        // 'boxSize = (width - (columns + 1) * space ) / columns'
        // and solve for boxSize:
        mBoxSize = SPACE_FACTOR * mWidth / (1f + mColumns * (SPACE_FACTOR + 1f));
        mSpace = mBoxSize / SPACE_FACTOR;
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mValid)
            return;

        float x = mSpace, y = mSpace;
        float maxX = mColumns * (mBoxSize + mSpace);
        for (int color : mColorList) {
            RectF rect = new RectF(x, y, x + mBoxSize, y + mBoxSize);
            mPaint.setColor(color);
            canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mPaint);
            x += mBoxSize + mSpace;
            if (x > maxX) {
                x = mSpace;
                y += mBoxSize + mSpace;
            }
        }
        drawSelectedRect(canvas);
    }

    private void drawSelectedRect(Canvas canvas) {
        float padding = mSpace / 2f;
        float l = mSelectedBounds.left;
        float t = mSelectedBounds.top;
        float r = mSelectedBounds.right;
        float b = mSelectedBounds.bottom;
        RectF rect = new RectF(l - padding, t - padding, r + padding, b + padding);
        Paint.Style paintStyle = mPaint.getStyle();
        setPaintStyleForSelectedBox();
        canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mPaint);
        mPaint.setStyle(paintStyle);
    }

    private void setPaintStyleForSelectedBox() {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(Color.WHITE);
    }

    private void setPaintStyleForBox() {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    @Override
    public int getSelectedColor() {
        return mColorList[mSelectedColorIndex];
    }

    @Override
    public boolean selectColor(float x, float y) {
        if (!mValid || mSelectedBounds.contains(x, y))
            return false;

        int column = (int) (x / (mBoxSize + mSpace));
        int row = (int) (y / (mBoxSize + mSpace));
        if (0 > row || row >= mRows || 0 > column || column >= mColumns)
            return false;

        int i = row * mColumns + column;
        if (i >= mColorList.length || i == mSelectedColorIndex)
            return false;

        float left = mSpace + column * (mBoxSize + mSpace);
        float top = mSpace + row * (mBoxSize + mSpace);
        if (left > x || x > left + mBoxSize || top > y || y > top + mBoxSize)
            return false;

        mSelectedBounds.set(left, top, left + mBoxSize, top + mBoxSize);
        mSelectedColorIndex = i;
        return true;
    }

    @Override
    public boolean selectColor(int color) {
        for (int i = 0; i < mColorList.length; ++i) {
            if (color == mColorList[i]) {
                if (mValid)
                    setSelectedColor(i);
                else
                    mSelectedColorIndex = i;
                return true;
            }
        }
        return false;
    }

    private void setSelectedColor(int i) {
        int row = i / mColumns;
        int column = i - row * mColumns;
        float x = mSpace + column * (mBoxSize + mSpace);
        float y = mSpace + row * (mBoxSize + mSpace);
        mSelectedBounds.set(x, y, x + mBoxSize, y + mBoxSize);
        mSelectedColorIndex = i;
    }
}
