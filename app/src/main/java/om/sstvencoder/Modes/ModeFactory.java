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

import java.lang.reflect.Constructor;

import om.sstvencoder.ModeInterfaces.IMode;
import om.sstvencoder.ModeInterfaces.IModeInfo;
import om.sstvencoder.ModeInterfaces.ModeSize;
import om.sstvencoder.Output.IOutput;

public final class ModeFactory {
    public static Class<?> getDefaultMode() {
        return Robot36.class;
    }

    public static IModeInfo[] getModeInfoList() {
        return new IModeInfo[]{
                new ModeInfo(Martin1.class), new ModeInfo(Martin2.class),
                new ModeInfo(PD50.class), new ModeInfo(PD90.class), new ModeInfo(PD120.class),
                new ModeInfo(PD160.class), new ModeInfo(PD180.class),
                new ModeInfo(PD240.class), new ModeInfo(PD290.class),
                new ModeInfo(Scottie1.class), new ModeInfo(Scottie2.class), new ModeInfo(ScottieDX.class),
                new ModeInfo(Robot36.class), new ModeInfo(Robot72.class),
                new ModeInfo(Wraase.class)
        };
    }

    public static IModeInfo getModeInfo(Class<?> modeClass) {
        if (!isModeClassValid(modeClass))
            return null;

        return new ModeInfo(modeClass);
    }

    public static IMode CreateMode(Class<?> modeClass, Bitmap bitmap, IOutput output) {
        Mode mode = null;

        if (bitmap != null && output != null && isModeClassValid(modeClass)) {
            ModeSize size = modeClass.getAnnotation(ModeSize.class);

            if (bitmap.getWidth() == size.width() && bitmap.getHeight() == size.height()) {
                try {
                    Constructor constructor = modeClass.getConstructor(Bitmap.class, IOutput.class);
                    mode = (Mode) constructor.newInstance(bitmap, output);
                } catch (Exception ignore) {
                }
            }
        }

        return mode;
    }

    private static boolean isModeClassValid(Class<?> modeClass) {
        return Mode.class.isAssignableFrom(modeClass) &&
                modeClass.isAnnotationPresent(ModeSize.class) &&
                modeClass.isAnnotationPresent(ModeDescription.class);
    }
}
