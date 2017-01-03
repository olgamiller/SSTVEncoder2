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

import om.sstvencoder.Modes.ImageFormats.Yuv;
import om.sstvencoder.Modes.ImageFormats.YuvFactory;
import om.sstvencoder.Modes.ImageFormats.YuvImageFormat;
import om.sstvencoder.ModeInterfaces.ModeSize;
import om.sstvencoder.Output.IOutput;
import om.sstvencoder.R;

@ModeSize(width = 320, height = 240)
@ModeDescription(name = R.string.action_robot72)
class Robot72 extends Mode {
    private final Yuv mYuv;

    private final int mLumaScanSamples;
    private final int mChrominanceScanSamples;

    private final int mSyncPulseSamples;
    private final double mSyncPulseFrequency;

    private final int mSyncPorchSamples;
    private final double mSyncPorchFrequency;

    private final int mPorchSamples;
    private final double mPorchFrequency;

    private final int mSeparatorSamples;
    private final double mFirstSeparatorFrequency;
    private final double mSecondSeparatorFrequency;

    Robot72(Bitmap bitmap, IOutput output) {
        super(bitmap, output);

        mYuv = YuvFactory.createYuv(mBitmap, YuvImageFormat.YUY2);
        mVISCode = 12;

        mLumaScanSamples = convertMsToSamples(138.0);
        mChrominanceScanSamples = convertMsToSamples(69.0);

        mSyncPulseSamples = convertMsToSamples(9.0);
        mSyncPulseFrequency = 1200.0;

        mSyncPorchSamples = convertMsToSamples(3.0);
        mSyncPorchFrequency = 1500.0;

        mPorchSamples = convertMsToSamples(1.5);
        mPorchFrequency = 1900.0;

        mSeparatorSamples = convertMsToSamples(4.5);
        mFirstSeparatorFrequency = 1500.0;
        mSecondSeparatorFrequency = 2300.0;
    }

    protected int getTransmissionSamples() {
        int lineSamples = mSyncPulseSamples + mSyncPorchSamples + mLumaScanSamples
                + 2 * (mSeparatorSamples + mPorchSamples + mChrominanceScanSamples);
        return mBitmap.getHeight() * lineSamples;
    }

    protected void writeEncodedLine() {
        addSyncPulse();
        addSyncPorch();
        addYScan(mLine);
        addSeparator(mFirstSeparatorFrequency);
        addPorch();
        addVScan(mLine);
        addSeparator(mSecondSeparatorFrequency);
        addPorch();
        addUScan(mLine);
    }

    private void addSyncPulse() {
        for (int i = 0; i < mSyncPulseSamples; ++i)
            setTone(mSyncPulseFrequency);
    }

    private void addSyncPorch() {
        for (int i = 0; i < mSyncPorchSamples; ++i)
            setTone(mSyncPorchFrequency);
    }

    private void addSeparator(double separatorFrequency) {
        for (int i = 0; i < mSeparatorSamples; ++i)
            setTone(separatorFrequency);
    }

    private void addPorch() {
        for (int i = 0; i < mPorchSamples; ++i)
            setTone(mPorchFrequency);
    }

    private void addYScan(int y) {
        for (int i = 0; i < mLumaScanSamples; ++i)
            setColorTone(mYuv.getY((i * mYuv.getWidth()) / mLumaScanSamples, y));
    }

    private void addUScan(int y) {
        for (int i = 0; i < mChrominanceScanSamples; ++i)
            setColorTone(mYuv.getU((i * mYuv.getWidth()) / mChrominanceScanSamples, y));
    }

    private void addVScan(int y) {
        for (int i = 0; i < mChrominanceScanSamples; ++i)
            setColorTone(mYuv.getV((i * mYuv.getWidth()) / mChrominanceScanSamples, y));
    }
}
