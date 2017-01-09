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
package om.sstvencoder;

import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

class ProgressBarWrapper {
    private final ProgressBar mProgressBar;
    private final TextView mText;
    private final Handler mHandler;
    private final int mSteps;
    private int mLastStep;
    private int mPosition, mMaxPosition;

    ProgressBarWrapper(ProgressBar progressBar, TextView text) {
        mProgressBar = progressBar;
        mProgressBar.setVisibility(View.GONE);
        mText = text;
        mText.setVisibility(View.GONE);
        mHandler = new Handler();
        mSteps = 10;
    }

    private void startProgressBar(final int max, final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setMax(max);
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.VISIBLE);
                if (text != null) {
                    mText.setText(text);
                    mText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void stepProgressBar(final int progress) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress(progress);
            }
        });
    }

    private void endProgressBar() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                mText.setVisibility(View.GONE);
            }
        });
    }

    void begin(int max, String text) {
        mLastStep = 0;
        mPosition = 0;
        mMaxPosition = max;
        startProgressBar(mSteps, text);
    }

    void step() {
        ++mPosition;
        int newStep = (mSteps * mPosition + mMaxPosition / 2) / mMaxPosition;
        if (newStep != mLastStep) {
            stepProgressBar(newStep);
            mLastStep = newStep;
        }
    }

    void end() {
        endProgressBar();
    }
}
