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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

class AudioOutput implements IOutput {
    private final double mSampleRate;
    private short[] mAudioBuffer;
    private AudioTrack mAudioTrack;
    private int mBufferPos;

    AudioOutput(double sampleRate) {
        mSampleRate = sampleRate;
        mBufferPos = 0;
    }

    @Override
    public void init(int samples) {
        mAudioBuffer = new short[(5 * (int) mSampleRate) / 2]; // 2.5 seconds of buffer
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                (int) mSampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mAudioBuffer.length * 2,
                AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    @Override
    public double getSampleRate() {
        return mSampleRate;
    }

    @Override
    public void write(double value) {
        if (mBufferPos == mAudioBuffer.length) {
            mAudioTrack.write(mAudioBuffer, 0, mAudioBuffer.length);
            mBufferPos = 0;
        }

        mAudioBuffer[mBufferPos++] = (short) (value * Short.MAX_VALUE);
    }

    @Override
    public void finish(boolean cancel) {
        if (mAudioTrack != null) {
            if (!cancel)
                drainBuffer();
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
            mAudioBuffer = null;
        }
    }

    private void drainBuffer() {
        // The second run makes sure that the previous buffer indeed got played
        for (int i = 0; i < 2; ++i) {
            while (mBufferPos < mAudioBuffer.length)
                mAudioBuffer[mBufferPos++] = 0;
            mAudioTrack.write(mAudioBuffer, 0, mAudioBuffer.length);
            mBufferPos = 0;
        }
    }
}
