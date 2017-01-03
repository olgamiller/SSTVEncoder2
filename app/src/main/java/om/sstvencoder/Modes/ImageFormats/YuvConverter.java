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

import android.graphics.Color;

final class YuvConverter {
    static int convertToY(int color) {
        double R = Color.red(color);
        double G = Color.green(color);
        double B = Color.blue(color);
        return clamp(16.0 + (.003906 * ((65.738 * R) + (129.057 * G) + (25.064 * B))));
    }

    static int convertToU(int color) {
        double R = Color.red(color);
        double G = Color.green(color);
        double B = Color.blue(color);
        return clamp(128.0 + (.003906 * ((-37.945 * R) + (-74.494 * G) + (112.439 * B))));
    }

    static int convertToV(int color) {
        double R = Color.red(color);
        double G = Color.green(color);
        double B = Color.blue(color);
        return clamp(128.0 + (.003906 * ((112.439 * R) + (-94.154 * G) + (-18.285 * B))));
    }

    private static int clamp(double value) {
        return value < 0.0 ? 0 : (value > 255.0 ? 255 : (int) value);
    }
}
