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
import android.graphics.Color;

import om.sstvencoder.ModeInterfaces.ModeSize;
import om.sstvencoder.Output.IOutput;

@ModeSize(width = 320, height = 256)
@ModeDescription(name = Wraase.Name)
class Wraase extends Mode {
    public static final String Name = "Wraase SC2 180";

    private final int mSyncPulseSamples;
    private final double mSyncPulseFrequency;

    private final int mPorchSamples;
    private final double mPorchFrequency;

    private final int mColorScanSamples;

    Wraase(Bitmap bitmap, IOutput output) {
        super(bitmap, output);

        mVISCode = 55;
        mColorScanSamples = convertMsToSamples(235.0);

        mSyncPulseSamples = convertMsToSamples(5.5225);
        mSyncPulseFrequency = 1200.0;

        mPorchSamples = convertMsToSamples(0.5);
        mPorchFrequency = 1500.0;
    }

    protected int getTransmissionSamples() {
        int lineSamples = mSyncPulseSamples + mPorchSamples + 3 * mColorScanSamples;
        return mBitmap.getHeight() * lineSamples;
    }

    protected void writeEncodedLine() {
        addSyncPulse();
        addPorch();
        addRedScan(mLine);
        addGreenScan(mLine);
        addBlueScan(mLine);
    }

    private void addSyncPulse() {
        for (int i = 0; i < mSyncPulseSamples; ++i)
            setTone(mSyncPulseFrequency);
    }

    private void addPorch() {
        for (int i = 0; i < mPorchSamples; ++i)
            setTone(mPorchFrequency);
    }

    private void addRedScan(int y) {
        for (int i = 0; i < mColorScanSamples; ++i)
            setColorTone(Color.red(getColor(i, y)));
    }

    private void addGreenScan(int y) {
        for (int i = 0; i < mColorScanSamples; ++i)
            setColorTone(Color.green(getColor(i, y)));
    }

    private void addBlueScan(int y) {
        for (int i = 0; i < mColorScanSamples; ++i)
            setColorTone(Color.blue(getColor(i, y)));
    }

    private int getColor(int colorScanSample, int y) {
        int x = colorScanSample * mBitmap.getWidth() / mColorScanSamples;
        return mBitmap.getPixel(x, y);
    }
}
