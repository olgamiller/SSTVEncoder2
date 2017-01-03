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
import android.net.Uri;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

class Settings {
    private final static String IMAGE_URI = "image_uri";
    private final static String TEXT_OVERLAY_PATH = "text_overlay_path";
    private final String mFileName;
    private Context mContext;
    private String mImageUri;
    private String mTextOverlayPath;

    private Settings() {
        mFileName = "settings.json";
    }

    Settings(Context context) {
        this();
        mContext = context;
        mImageUri = "";
    }

    boolean load() {
        boolean loaded = false;
        JsonReader reader = null;
        try {
            InputStream in = new FileInputStream(getFile());
            reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            readImageUri(reader);
            readTextOverlayPath(reader);
            loaded = true;
        } catch (Exception ignore) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {
                }
            }
        }
        return loaded;
    }

    boolean save() {
        boolean saved = false;
        JsonWriter writer = null;
        try {
            OutputStream out = new FileOutputStream(getFile());
            writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.setIndent(" ");
            writeImageUri(writer);
            writeTextOverlayPath(writer);
            saved = true;
        } catch (Exception ignore) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {
                }
            }
        }
        return saved;
    }

    void setImageUri(Uri uri) {
        mImageUri = uri == null ? "" : uri.toString();
    }

    Uri getImageUri() {
        if ("".equals(mImageUri))
            return null;
        return Uri.parse(mImageUri);
    }

    File getTextOverlayFile() {
        if (mTextOverlayPath == null)
            mTextOverlayPath = new File(mContext.getFilesDir(), "text_overlay.json").getPath();
        return new File(mTextOverlayPath);
    }

    private File getFile() {
        return new File(mContext.getFilesDir(), mFileName);
    }

    private void writeImageUri(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(IMAGE_URI).value(mImageUri);
        writer.endObject();
    }

    private void writeTextOverlayPath(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(TEXT_OVERLAY_PATH).value(mTextOverlayPath);
        writer.endObject();
    }

    private void readImageUri(JsonReader reader) throws IOException {
        reader.beginObject();
        {
            reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                mImageUri = null;
            } else
                mImageUri = reader.nextString();
        }
        reader.endObject();
    }

    private void readTextOverlayPath(JsonReader reader) throws IOException {
        reader.beginObject();
        {
            reader.nextName();
            mTextOverlayPath = reader.nextString();
        }
        reader.endObject();
    }
}
