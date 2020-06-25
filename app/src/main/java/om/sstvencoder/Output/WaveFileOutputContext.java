/*
Copyright 2020 Olga Miller <olga.rgb@gmail.com>

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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class WaveFileOutputContext {
    private ContentResolver mContentResolver;
    private String mFileName;
    private File mFile;
    private Uri mUri;
    private ContentValues mValues;

    public WaveFileOutputContext(ContentResolver contentResolver, String fileName) {
        mContentResolver = contentResolver;
        mFileName = fileName;
    }

    public String getFileName() {
        return mFileName;
    }

    public OutputStream createWaveOutputStream() {
        if (init()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    return mContentResolver.openOutputStream(mUri);
                else
                    return new FileOutputStream(mFile);
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private boolean init() {
        mValues = getContentValues(mFileName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mUri = mContentResolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), mValues);
            if (mUri != null) {
                String path = mUri.getPath();
                if (path != null)
                    mFile = new File(path);
            }
        } else {
            mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), mFileName);
        }
        return mUri != null;
    }

    private ContentValues getContentValues(String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, (new File(Environment.DIRECTORY_MUSIC, "SSTV Encoder")).getPath());
            values.put(MediaStore.Audio.Media.IS_PENDING, 1);
        } else {
            values.put(MediaStore.Audio.Media.ALBUM, "SSTV Encoder");
            values.put(MediaStore.Audio.Media.TITLE, fileName);
            values.put(MediaStore.Audio.Media.IS_MUSIC, true);
        }
        return values;
    }

    public void clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mUri != null && mValues != null) {
                mValues.clear();
                mValues.put(MediaStore.Audio.Media.IS_PENDING, 0);
                mContentResolver.update(mUri, mValues, null, null);
            }
        } else {
            if (mFile != null && mValues != null) {
                mValues.put(MediaStore.Audio.Media.DATA, mFile.toString());
                mUri = mContentResolver.insert(MediaStore.Audio.Media.getContentUriForPath(mFile.getAbsolutePath()), mValues);
            }
        }
    }

    public void deleteFile() {
        try {
            if (mFile != null)
                mFile.delete();
        } catch (Exception ignore) {
        }
    }
}
