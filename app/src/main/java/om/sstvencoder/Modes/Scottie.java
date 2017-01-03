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

import om.sstvencoder.Output.IOutput;

abstract class Scottie extends Mode {
    private final int mSyncPulseSamples;
    private final double mSyncPulseFrequency;

    private final int mSyncPorchSamples;
    private final double mSyncPorchFrequency;

    private final int mSeparatorSamples;
    private final double mSeparatorFrequency;

    protected double mColorScanDurationMs;
    protected int mColorScanSamples;

    Scottie(Bitmap bitmap, IOutput output) {
        super(bitmap, output);

        mSyncPulseSamples = convertMsToSamples(9.0);
        mSyncPulseFrequency = 1200.0;

        mSyncPorchSamples = convertMsToSamples(1.5);
        mSyncPorchFrequency = 1500.0;

        mSeparatorSamples = convertMsToSamples(1.5);
        mSeparatorFrequency = 1500.0;
    }

    protected int getTransmissionSamples() {
        int lineSamples = 2 * mSeparatorSamples + 3 * mColorScanSamples +
                mSyncPulseSamples + mSyncPorchSamples;
        return mSyncPulseSamples + mBitmap.getHeight() * lineSamples;
    }

    protected void writeEncodedLine() {
        if (mLine == 0)
            addSyncPulse();

        addSeparator();
        addGreenScan(mLine);
        addSeparator();
        addBlueScan(mLine);
        addSyncPulse();
        addSyncPorch();
        addRedScan(mLine);
    }

    private void addSyncPulse() {
        for (int i = 0; i < mSyncPulseSamples; ++i)
            setTone(mSyncPulseFrequency);
    }

    private void addSyncPorch() {
        for (int i = 0; i < mSyncPorchSamples; ++i)
            setTone(mSyncPorchFrequency);
    }

    private void addSeparator() {
        for (int i = 0; i < mSeparatorSamples; ++i)
            setTone(mSeparatorFrequency);
    }

    private void addGreenScan(int y) {
        for (int i = 0; i < mColorScanSamples; ++i)
            setColorTone(Color.green(getColor(i, y)));
    }

    private void addBlueScan(int y) {
        for (int i = 0; i < mColorScanSamples; ++i)
            setColorTone(Color.blue(getColor(i, y)));
    }

    private void addRedScan(int y) {
        for (int i = 0; i < mColorScanSamples; ++i)
            setColorTone(Color.red(getColor(i, y)));
    }

    private int getColor(int colorScanSample, int y) {
        int x = colorScanSample * mBitmap.getWidth() / mColorScanSamples;
        return mBitmap.getPixel(x, y);
    }
}
