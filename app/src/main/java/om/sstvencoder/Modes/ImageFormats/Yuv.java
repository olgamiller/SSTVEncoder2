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
package om.sstvencoder.Modes.ImageFormats;

import android.graphics.Bitmap;

public abstract class Yuv {
    protected byte[] mYuv;
    final int mWidth;
    final int mHeight;

    Yuv(Bitmap bitmap) {
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        convertBitmapToYuv(bitmap);
    }

    protected abstract void convertBitmapToYuv(Bitmap bitmap);

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public abstract int getY(int x, int y);

    public abstract int getU(int x, int y);

    public abstract int getV(int x, int y);
}
