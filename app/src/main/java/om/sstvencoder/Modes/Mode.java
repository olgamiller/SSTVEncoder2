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

import om.sstvencoder.ModeInterfaces.IMode;
import om.sstvencoder.Output.IOutput;

abstract class Mode implements IMode {
    protected Bitmap mBitmap;
    protected int mVISCode;
    protected int mLine;
    private IOutput mOutput;
    private double mSampleRate;
    private double mRunningIntegral;

    protected Mode(Bitmap bitmap, IOutput output) {
        mOutput = output;
        mSampleRate = mOutput.getSampleRate();
        mBitmap = bitmap;
    }

    @Override
    public void init() {
        mRunningIntegral = 0.0;
        mLine = 0;
        mOutput.init(getTotalSamples());
        writeCalibrationHeader();
    }

    @Override
    public int getProcessCount() {
        return mBitmap.getHeight();
    }

    @Override
    public boolean process() {
        if (mLine >= mBitmap.getHeight())
            return false;

        writeEncodedLine();
        ++mLine;
        return true;
    }

    // Note that also Bitmap will be recycled here
    @Override
    public void finish(boolean cancel) {
        mOutput.finish(cancel);
        destroyBitmap();
    }

    private int getTotalSamples() {
        return getHeaderSamples() + getTransmissionSamples();
    }

    private int getHeaderSamples() {
        return 2 * convertMsToSamples(300.0)
                + convertMsToSamples(10.0)
                + 10 * convertMsToSamples(30.0);
    }

    protected abstract int getTransmissionSamples();

    private void writeCalibrationHeader() {
        int leaderToneSamples = convertMsToSamples(300.0);
        double leaderToneFrequency = 1900.0;

        int breakSamples = convertMsToSamples(10.0);
        double breakFrequency = 1200.0;

        int visBitSamples = convertMsToSamples(30.0);
        double visBitSSFrequency = 1200.0;
        double[] visBitFrequency = new double[]{1300.0, 1100.0};

        for (int i = 0; i < leaderToneSamples; ++i)
            setTone(leaderToneFrequency);

        for (int i = 0; i < breakSamples; ++i)
            setTone(breakFrequency);

        for (int i = 0; i < leaderToneSamples; ++i)
            setTone(leaderToneFrequency);

        for (int i = 0; i < visBitSamples; ++i)
            setTone(visBitSSFrequency);

        int parity = 0;
        for (int pos = 0; pos < 7; ++pos) {
            int bit = (mVISCode >> pos) & 1;
            parity ^= bit;
            for (int i = 0; i < visBitSamples; ++i)
                setTone(visBitFrequency[bit]);
        }

        for (int i = 0; i < visBitSamples; ++i)
            setTone(visBitFrequency[parity]);

        for (int i = 0; i < visBitSamples; ++i)
            setTone(visBitSSFrequency);
    }

    protected abstract void writeEncodedLine();

    protected int convertMsToSamples(double durationMs) {
        return (int) Math.round(durationMs * mSampleRate / 1000.0);
    }

    protected void setTone(double frequency) {
        mRunningIntegral += 2.0 * frequency * Math.PI / mSampleRate;
        mRunningIntegral %= 2.0 * Math.PI;
        mOutput.write(Math.sin(mRunningIntegral));
    }

    protected void setColorTone(int color) {
        double blackFrequency = 1500.0;
        double whiteFrequency = 2300.0;
        setTone(color * (whiteFrequency - blackFrequency) / 255.0 + blackFrequency);
    }

    private void destroyBitmap() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
