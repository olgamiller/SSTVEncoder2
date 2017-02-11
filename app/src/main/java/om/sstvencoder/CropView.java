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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import om.sstvencoder.ModeInterfaces.ModeSize;
import om.sstvencoder.TextOverlay.Label;
import om.sstvencoder.TextOverlay.LabelCollection;

public class CropView extends ImageView {
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mLongPress) {
                moveImage(distanceX, distanceY);
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mLongPress = false;
            if (!mInScale && mLabelCollection.moveLabelBegin(e.getX(), e.getY())) {
                invalidate();
                mLongPress = true;
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!mLongPress) {
                editLabelBegin(e.getX(), e.getY());
                return true;
            }
            return false;
        }
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (!mLongPress) {
                mInScale = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleImage(detector.getScaleFactor());
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mInScale = false;
        }
    }

    private GestureDetectorCompat mDetectorCompat;
    private ScaleGestureDetector mScaleDetector;
    private boolean mLongPress, mInScale;
    private ModeSize mModeSize;
    private final Paint mPaint, mRectPaint, mBorderPaint;
    private RectF mInputRect;
    private Rect mOutputRect;
    private BitmapRegionDecoder mRegionDecoder;
    private int mImageWidth, mImageHeight;
    private Bitmap mCacheBitmap;
    private boolean mSmallImage;
    private boolean mImageOK;
    private final Rect mCanvasDrawRect, mImageDrawRect;
    private int mOrientation;
    private Rect mCacheRect;
    private int mCacheSampleSize;
    private final BitmapFactory.Options mBitmapOptions;
    private LabelCollection mLabelCollection;

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDetectorCompat = new GestureDetectorCompat(getContext(), new GestureListener());
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());

        mBitmapOptions = new BitmapFactory.Options();

        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mRectPaint = new Paint();
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(1f);
        mBorderPaint = new Paint();
        mBorderPaint.setColor(Color.BLACK);

        mCanvasDrawRect = new Rect();
        mImageDrawRect = new Rect();
        mCacheRect = new Rect();
        mOutputRect = new Rect();

        mSmallImage = false;
        mImageOK = false;

        mLabelCollection = new LabelCollection();
    }

    public void setModeSize(ModeSize size) {
        mModeSize = size;
        mOutputRect = Utility.getEmbeddedRect(getWidth(), getHeight(), mModeSize.width(), mModeSize.height());
        if (mImageOK)
            resetInputRect();
        invalidate();
    }

    private void resetInputRect() {
        float iw = mModeSize.width();
        float ih = mModeSize.height();
        float ow = mImageWidth;
        float oh = mImageHeight;
        if (iw * oh > ow * ih) {
            mInputRect = new RectF(0.0f, 0.0f, (iw * oh) / ih, oh);
            mInputRect.offset((ow - (iw * oh) / ih) / 2.0f, 0.0f);
        } else {
            mInputRect = new RectF(0.0f, 0.0f, ow, (ih * ow) / iw);
            mInputRect.offset(0.0f, (oh - (ih * ow) / iw) / 2.0f);
        }
    }

    public void rotateImage(int orientation) {
        if (!mImageOK)
            return;
        mOrientation += orientation;
        mOrientation %= 360;
        if (orientation == 90 || orientation == 270) {
            int tmp = mImageWidth;
            mImageWidth = mImageHeight;
            mImageHeight = tmp;
        }
        resetInputRect();
        invalidate();
    }

    public void setNoBitmap() {
        mImageOK = false;
        mOrientation = 0;
        recycle();
        invalidate();
    }

    public void setBitmap(@NonNull InputStream stream) throws IOException, IllegalArgumentException {
        mImageOK = false;
        mOrientation = 0;
        recycle();
        loadImage(stream);
        invalidate();
    }

    private void loadImage(InputStream stream) throws IOException, IllegalArgumentException {
        // app6 + exif
        int bufferBytes = 1048576;
        if (!stream.markSupported())
            stream = new BufferedInputStream(stream, bufferBytes);
        stream.mark(bufferBytes);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new BufferedInputStream(stream), null, options);
        stream.reset();
        mImageWidth = options.outWidth;
        mImageHeight = options.outHeight;

        if (mImageWidth * mImageHeight < 1024 * 1024) {
            mCacheBitmap = BitmapFactory.decodeStream(stream);
            mSmallImage = true;
        } else {
            mRegionDecoder = BitmapRegionDecoder.newInstance(stream, true);
            mCacheRect.setEmpty();
            mSmallImage = false;
        }

        if (mCacheBitmap == null && mRegionDecoder == null) {
            String size = options.outWidth + "x" + options.outHeight;
            String message = "Stream could not be decoded. Image size: " + size;
            if (mImageWidth <= 0 || mImageHeight <= 0)
                throw new IllegalArgumentException(message);
            else
                throw new IOException(message);
        }

        mImageOK = true;
        resetInputRect();
    }

    private void recycle() {
        if (mRegionDecoder != null) {
            mRegionDecoder.recycle();
            mRegionDecoder = null;
        }
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
            mCacheBitmap = null;
        }
    }

    public void scaleImage(float scaleFactor) {
        if (!mImageOK)
            return;
        float newW = mInputRect.width() / scaleFactor;
        float newH = mInputRect.height() / scaleFactor;
        float dx = 0.5f * (mInputRect.width() - newW);
        float dy = 0.5f * (mInputRect.height() - newH);
        float max = 2.0f * Math.max(mImageWidth, mImageHeight);
        if (Math.min(newW, newH) >= 4.0f && Math.max(newW, newH) <= max) {
            mInputRect.inset(dx, dy);
            invalidate();
        }
    }

    public void moveImage(float distanceX, float distanceY) {
        if (!mImageOK)
            return;
        float dx = (mInputRect.width() * distanceX) / mOutputRect.width();
        float dy = (mInputRect.height() * distanceY) / mOutputRect.height();
        dx = Math.max(mInputRect.width() * 0.1f, mInputRect.right + dx) - mInputRect.right;
        dy = Math.max(mInputRect.height() * 0.1f, mInputRect.bottom + dy) - mInputRect.bottom;
        dx = Math.min(mImageWidth - mInputRect.width() * 0.1f, mInputRect.left + dx) - mInputRect.left;
        dy = Math.min(mImageHeight - mInputRect.height() * 0.1f, mInputRect.top + dy) - mInputRect.top;
        mInputRect.offset(dx, dy);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e) {
        boolean consumed = false;
        if (mLongPress) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    mLabelCollection.moveLabel(e.getX(), e.getY());
                    invalidate();
                    consumed = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mLabelCollection.moveLabelEnd();
                    invalidate();
                    mLongPress = false;
                    consumed = true;
                    break;
            }
        }
        consumed = mScaleDetector.onTouchEvent(e) || consumed;
        return mDetectorCompat.onTouchEvent(e) || consumed || super.onTouchEvent(e);
    }

    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);
        if (mModeSize != null)
            mOutputRect = Utility.getEmbeddedRect(w, h, mModeSize.width(), mModeSize.height());
        mLabelCollection.update(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mImageOK) {
            maximizeImageToCanvasRect();
            adjustCanvasAndImageRect(getWidth(), getHeight());
            canvas.drawRect(mOutputRect, mBorderPaint);
            drawBitmap(canvas);
        }
        mLabelCollection.draw(canvas);
        drawModeRect(canvas);
    }

    private void maximizeImageToCanvasRect() {
        mImageDrawRect.left = Math.round(mInputRect.left - mOutputRect.left * mInputRect.width() / mOutputRect.width());
        mImageDrawRect.top = Math.round(mInputRect.top - mOutputRect.top * mInputRect.height() / mOutputRect.height());
        mImageDrawRect.right = Math.round(mInputRect.right - (mOutputRect.right - getWidth()) * mInputRect.width() / mOutputRect.width());
        mImageDrawRect.bottom = Math.round(mInputRect.bottom - (mOutputRect.bottom - getHeight()) * mInputRect.height() / mOutputRect.height());
    }

    private void adjustCanvasAndImageRect(int width, int height) {
        mCanvasDrawRect.set(0, 0, width, height);
        if (mImageDrawRect.left < 0) {
            mCanvasDrawRect.left -= (mImageDrawRect.left * mCanvasDrawRect.width()) / mImageDrawRect.width();
            mImageDrawRect.left = 0;
        }
        if (mImageDrawRect.top < 0) {
            mCanvasDrawRect.top -= (mImageDrawRect.top * mCanvasDrawRect.height()) / mImageDrawRect.height();
            mImageDrawRect.top = 0;
        }
        if (mImageDrawRect.right > mImageWidth) {
            mCanvasDrawRect.right -= ((mImageDrawRect.right - mImageWidth) * mCanvasDrawRect.width()) / mImageDrawRect.width();
            mImageDrawRect.right = mImageWidth;
        }
        if (mImageDrawRect.bottom > mImageHeight) {
            mCanvasDrawRect.bottom -= ((mImageDrawRect.bottom - mImageHeight) * mCanvasDrawRect.height()) / mImageDrawRect.height();
            mImageDrawRect.bottom = mImageHeight;
        }
    }

    private void drawModeRect(Canvas canvas) {
        mRectPaint.setColor(Color.BLUE);
        canvas.drawRect(mOutputRect, mRectPaint);
        mRectPaint.setColor(Color.GREEN);
        drawRectInset(canvas, mOutputRect, -1);
        mRectPaint.setColor(Color.RED);
        drawRectInset(canvas, mOutputRect, -2);
    }

    private void drawRectInset(Canvas canvas, Rect rect, int inset) {
        canvas.drawRect(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset, mRectPaint);
    }

    private Rect getIntRect(RectF rect) {
        return new Rect(Math.round(rect.left), Math.round(rect.top), Math.round(rect.right), Math.round(rect.bottom));
    }

    private int getSampleSize() {
        int sx = Math.round(mInputRect.width() / mModeSize.width());
        int sy = Math.round(mInputRect.height() / mModeSize.height());
        int scale = Math.max(1, Math.max(sx, sy));
        return Integer.highestOneBit(scale);
    }

    public Bitmap getBitmap() {
        Bitmap result = Bitmap.createBitmap(mModeSize.width(), mModeSize.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.BLACK);
        if (mImageOK) {
            mImageDrawRect.set(getIntRect(mInputRect));
            adjustCanvasAndImageRect(mModeSize.width(), mModeSize.height());
            drawBitmap(canvas);
        }
        mLabelCollection.draw(canvas, mOutputRect, new Rect(0, 0, mModeSize.width(), mModeSize.height()));
        return result;
    }

    private void drawBitmap(Canvas canvas) {
        int w = mImageWidth;
        int h = mImageHeight;
        for (int i = 0; i < mOrientation / 90; ++i) {
            int tmp = w;
            w = h;
            h = tmp;
            mImageDrawRect.set(mImageDrawRect.top, h - mImageDrawRect.left, mImageDrawRect.bottom, h - mImageDrawRect.right);
            mCanvasDrawRect.set(mCanvasDrawRect.top, -mCanvasDrawRect.right, mCanvasDrawRect.bottom, -mCanvasDrawRect.left);
        }
        mImageDrawRect.sort();
        canvas.save();
        canvas.rotate(mOrientation);
        if (!mSmallImage) {
            int sampleSize = getSampleSize();
            if (sampleSize < mCacheSampleSize || !mCacheRect.contains(mImageDrawRect)) {
                if (mCacheBitmap != null)
                    mCacheBitmap.recycle();
                int cacheWidth = mImageDrawRect.width();
                int cacheHeight = mImageDrawRect.height();
                while (cacheWidth * cacheHeight < (sampleSize * 1024 * sampleSize * 1024)) {
                    cacheWidth += mImageDrawRect.width();
                    cacheHeight += mImageDrawRect.height();
                }
                mCacheRect.set(
                        Math.max(0, ~(sampleSize - 1) & (mImageDrawRect.centerX() - cacheWidth / 2)),
                        Math.max(0, ~(sampleSize - 1) & (mImageDrawRect.centerY() - cacheHeight / 2)),
                        Math.min(mRegionDecoder.getWidth(), ~(sampleSize - 1) & (mImageDrawRect.centerX() + cacheWidth / 2 + sampleSize - 1)),
                        Math.min(mRegionDecoder.getHeight(), ~(sampleSize - 1) & (mImageDrawRect.centerY() + cacheHeight / 2 + sampleSize - 1)));
                mBitmapOptions.inSampleSize = mCacheSampleSize = sampleSize;
                mCacheBitmap = mRegionDecoder.decodeRegion(mCacheRect, mBitmapOptions);
            }
            mImageDrawRect.offset(-mCacheRect.left, -mCacheRect.top);
            mImageDrawRect.left /= mCacheSampleSize;
            mImageDrawRect.top /= mCacheSampleSize;
            mImageDrawRect.right /= mCacheSampleSize;
            mImageDrawRect.bottom /= mCacheSampleSize;
        }
        canvas.drawBitmap(mCacheBitmap, mImageDrawRect, mCanvasDrawRect, mPaint);
        canvas.restore();
    }

    private void editLabelBegin(float x, float y) {
        Label label = mLabelCollection.editLabelBegin(x, y);
        ((MainActivity) getContext()).startEditTextActivity(label);
    }

    public void editLabelEnd(Label label) {
        mLabelCollection.editLabelEnd(label);
        invalidate();
    }

    public LabelCollection getLabels() {
        return mLabelCollection;
    }
}
