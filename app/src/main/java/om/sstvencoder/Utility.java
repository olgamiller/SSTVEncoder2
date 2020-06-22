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

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Utility {
    @NonNull
    static Rect getEmbeddedRect(int w, int h, int iw, int ih) {
        Rect rect;

        int ow = (9 * w) / 10;
        int oh = (9 * h) / 10;

        if (iw * oh < ow * ih) {
            int right = (iw * oh) / ih;
            rect = new Rect(0, 0, right, oh);
            rect.offset((w - right) / 2, (h - oh) / 2);
        } else {
            int bottom = (ih * ow) / iw;
            rect = new Rect(0, 0, ow, bottom);
            rect.offset((w - ow) / 2, (h - bottom) / 2);
        }
        return rect;
    }

    static float getTextSizeFactor(int w, int h) {
        return 0.1f * (Utility.getEmbeddedRect(w, h, 320, 240).height());
    }

    static String createMessage(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getMessage());
        sb.append("\n");
        for (StackTraceElement el : ex.getStackTrace()) {
            sb.append("\n");
            sb.append(el.toString());
        }
        return sb.toString();
    }

    @NonNull
    static Intent createEmailIntent(final String subject, final String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/email");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"olga.rgb@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    static int convertToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
        }
        return 0;
    }

    static Uri createImageUri(Context context) {
        if (!isExternalStorageWritable())
            return null;
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File file = new File(dir, createFileName() + ".jpg");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            // API level 24 and higher: FileUriExposedException
            return Uri.fromFile(file); // file:// URI
        // API level 15: Camera crash
        return FileProvider.getUriForFile(context, "om.sstvencoder", file); // content:// URI
    }

    static String createWaveFileName() {
        return createFileName() + ".wav";
    }

    private static String createFileName() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
