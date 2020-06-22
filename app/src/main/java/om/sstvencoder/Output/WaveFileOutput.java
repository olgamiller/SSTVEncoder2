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
package om.sstvencoder.Output;

import java.io.BufferedOutputStream;

class WaveFileOutput implements IOutput {
    private final double mSampleRate;
    private WaveFileOutputContext mContext;
    private BufferedOutputStream mOutputStream;
    private int mSamples, mWrittenSamples;

    WaveFileOutput(WaveFileOutputContext context, double sampleRate) {
        mContext = context;
        mSampleRate = sampleRate;
    }

    public void init(int samples) {
        int offset = (int) ((0.01 * mSampleRate) / 2.0);
        mSamples = samples + 2 * offset;
        mWrittenSamples = 0;
        InitOutputStream();
        writeHeader();
        padWithZeros(offset);
    }

    private void writeHeader() {
        try {
            int numChannels = 1; // mono
            int bitsPerSample = Short.SIZE;
            int blockAlign = numChannels * bitsPerSample / Byte.SIZE;
            int subchunk2Size = mSamples * blockAlign;

            mOutputStream.write("RIFF".getBytes()); // ChunkID
            mOutputStream.write(toLittleEndian(36 + subchunk2Size)); // ChunkSize
            mOutputStream.write("WAVE".getBytes()); // Format

            mOutputStream.write("fmt ".getBytes()); // Subchunk1ID
            mOutputStream.write(toLittleEndian(16)); // Subchunk1Size
            mOutputStream.write(toLittleEndian((short) 1)); // AudioFormat
            mOutputStream.write(toLittleEndian((short) numChannels)); // NumChannels
            mOutputStream.write(toLittleEndian((int) mSampleRate)); // SampleRate
            mOutputStream.write(toLittleEndian((int) mSampleRate * blockAlign)); // ByteRate
            mOutputStream.write(toLittleEndian((short) blockAlign)); // BlockAlign
            mOutputStream.write(toLittleEndian((short) bitsPerSample)); // BitsPerSample

            mOutputStream.write("data".getBytes()); // Subchunk2ID
            mOutputStream.write(toLittleEndian(subchunk2Size)); // Subchunk2Size
        } catch (Exception ignore) {
        }
    }

    private void InitOutputStream() {
        try {
            mOutputStream = new BufferedOutputStream(mContext.createWaveOutputStream());
        } catch (Exception ignore) {
        }
    }

    @Override
    public double getSampleRate() {
        return mSampleRate;
    }

    @Override
    public void write(double value) {
        short tmp = (short) (value * Short.MAX_VALUE);
        ++mWrittenSamples;
        try {
            mOutputStream.write(toLittleEndian(tmp));
        } catch (Exception ignore) {
        }
    }

    @Override
    public void finish(boolean cancel) {
        if (!cancel)
            padWithZeros(mSamples);

        try {
            mOutputStream.close();
            mOutputStream = null;
        } catch (Exception ignore) {
        }

        if (cancel)
            mContext.deleteFile();
    }

    private void padWithZeros(int count) {
        try {
            while (mWrittenSamples++ < count)
                mOutputStream.write(toLittleEndian((short) 0));
        } catch (Exception ignore) {
        }
    }

    private byte[] toLittleEndian(int value) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) (value & 255);
        buffer[1] = (byte) ((value >> 8) & 255);
        buffer[2] = (byte) ((value >> 16) & 255);
        buffer[3] = (byte) ((value >> 24) & 255);
        return buffer;
    }

    private byte[] toLittleEndian(short value) {
        byte[] buffer = new byte[2];
        buffer[0] = (byte) (value & 255);
        buffer[1] = (byte) ((value >> 8) & 255);
        return buffer;
    }
}
