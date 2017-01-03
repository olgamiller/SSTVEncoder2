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

import om.sstvencoder.ModeInterfaces.IModeInfo;
import om.sstvencoder.ModeInterfaces.ModeSize;

class ModeInfo implements IModeInfo {
    private final Class<?> mModeClass;

    ModeInfo(Class<?> modeClass) {
        mModeClass = modeClass;
    }

    public int getModeName() {
        return mModeClass.getAnnotation(ModeDescription.class).name();
    }

    public String getModeClassName() {
        return mModeClass.getName();
    }

    public ModeSize getModeSize() {
        return mModeClass.getAnnotation(ModeSize.class);
    }
}
