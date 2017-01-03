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

import android.graphics.Bitmap;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import om.sstvencoder.ModeInterfaces.IMode;
import om.sstvencoder.ModeInterfaces.IModeInfo;
import om.sstvencoder.Modes.ModeFactory;
import om.sstvencoder.Output.IOutput;
import om.sstvencoder.Output.OutputFactory;

// Creates IMode instance
class Encoder {
    private final Thread mThread;
    private final List<IMode> mQueue;
    private boolean mQuit, mStop;
    private Class<?> mModeClass;

    Encoder() {
        mQueue = new LinkedList<>();
        mQuit = false;
        mStop = false;
        mModeClass = ModeFactory.getDefaultMode();

        mThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    IMode mode;
                    synchronized (this) {
                        while (mQueue.isEmpty() && !mQuit) {
                            try {
                                wait();
                            } catch (Exception ignore) {
                            }
                        }
                        if (mQuit)
                            return;

                        mStop = false;
                        mode = mQueue.remove(0);
                    }
                    mode.init();

                    while (mode.process()) {
                        synchronized (this) {
                            if (mQuit || mStop)
                                break;
                        }
                    }
                    mode.finish(mStop);
                }
            }
        };
        mThread.start();
    }

    boolean setMode(String className) {
        try {
            mModeClass = Class.forName(className);
        } catch (Exception ignore) {
            return false;
        }
        return true;
    }

    IModeInfo getModeInfo() {
        return ModeFactory.getModeInfo(mModeClass);
    }

    IModeInfo[] getModeInfoList() {
        return ModeFactory.getModeInfoList();
    }

    void play(Bitmap bitmap) {
        IOutput output = OutputFactory.createOutputForSending();
        IMode mode = ModeFactory.CreateMode(mModeClass, bitmap, output);
        if (mode != null)
            enqueue(mode);
    }

    boolean save(Bitmap bitmap, File file) {
        IOutput output = OutputFactory.createOutputForSavingAsWave(file);
        IMode mode = ModeFactory.CreateMode(mModeClass, bitmap, output);
        if (mode != null) {
            mode.init();

            while (mode.process()) {
                if (mQuit)
                    break;
            }
            mode.finish(mQuit);
        }
        return !mQuit;
    }

    void stop() {
        synchronized (mThread) {
            mStop = true;
            int size = mQueue.size();
            for (int i = 0; i < size; ++i)
                mQueue.remove(0).finish(true);
        }
    }

    private void enqueue(IMode mode) {
        synchronized (mThread) {
            mQueue.add(mode);
            mThread.notify();
        }
    }

    void destroy() {
        synchronized (mThread) {
            mQuit = true;
            mThread.notify();
        }
    }
}
