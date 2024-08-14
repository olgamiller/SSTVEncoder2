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
package om.sstvencoder.Modes;

import android.graphics.Bitmap;

import om.sstvencoder.ModeInterfaces.ModeSize;
import om.sstvencoder.Output.IOutput;

@ModeSize(width = 640, height = 496)
@ModeDescription(name = PD180.Name)
class PD180 extends PD {
    public static final String Name = "PD 180";

    PD180(Bitmap bitmap, IOutput output) {
        super(bitmap, output);
        mVISCode = 96;
        mColorScanDurationMs = 183.04;
        mColorScanSamples = convertMsToSamples(mColorScanDurationMs);
    }
}

