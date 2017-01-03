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

public final class YuvFactory {
    public static Yuv createYuv(Bitmap bitmap, int format) {
        switch (format) {
            case YuvImageFormat.YV12:
                return new YV12(bitmap);
            case YuvImageFormat.NV21:
                return new NV21(bitmap);
            case YuvImageFormat.YUY2:
                return new YUY2(bitmap);
            case YuvImageFormat.YUV440P:
                return new YUV440P(bitmap);
            default:
                throw new IllegalArgumentException("Only support YV12, NV21, YUY2 and YUV440P");
        }
    }
}
