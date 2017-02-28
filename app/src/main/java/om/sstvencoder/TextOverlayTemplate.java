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

import android.support.annotation.NonNull;
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

import om.sstvencoder.TextOverlay.IReader;
import om.sstvencoder.TextOverlay.IWriter;
import om.sstvencoder.TextOverlay.LabelCollection;

class TextOverlayTemplate {
    private class LabelCollectionWriter implements IWriter {
        private JsonWriter mWriter;

        private LabelCollectionWriter(@NonNull JsonWriter writer) {
            mWriter = writer;
        }

        @Override
        public void beginRootObject() throws IOException {
            mWriter.beginObject();
        }

        @Override
        public void beginObject(@NonNull String name) throws IOException {
            mWriter.name(name);
            mWriter.beginObject();
        }

        @Override
        public void endObject() throws IOException {
            mWriter.endObject();
        }

        @Override
        public void beginArray(@NonNull String name) throws IOException {
            mWriter.name(name);
            mWriter.beginArray();
        }

        @Override
        public void endArray() throws IOException {
            mWriter.endArray();
        }

        @Override
        public void write(@NonNull String name, String value) throws IOException {
            mWriter.name(name).value(value);
        }

        @Override
        public void write(@NonNull String name, boolean value) throws IOException {
            mWriter.name(name).value(value);
        }

        @Override
        public void write(@NonNull String name, float value) throws IOException {
            mWriter.name(name).value(value);
        }

        @Override
        public void write(@NonNull String name, int value) throws IOException {
            mWriter.name(name).value(value);
        }
    }

    private class LabelCollectionReader implements IReader {
        private JsonReader mReader;

        private LabelCollectionReader(@NonNull JsonReader reader) {
            mReader = reader;
        }

        @Override
        public void beginRootObject() throws IOException {
            mReader.beginObject();
        }

        @Override
        public void beginObject() throws IOException {
            mReader.nextName();
            mReader.beginObject();
        }

        @Override
        public void endObject() throws IOException {
            mReader.endObject();
        }

        @Override
        public void beginArray() throws IOException {
            mReader.nextName();
            mReader.beginArray();
        }

        @Override
        public void endArray() throws IOException {
            mReader.endArray();
        }

        @Override
        public boolean hasNext() throws IOException {
            return mReader.hasNext();
        }

        @Override
        public String readString() throws IOException {
            mReader.nextName();
            if (mReader.peek() == JsonToken.NULL) {
                mReader.nextNull();
                return null;
            }
            return mReader.nextString();
        }

        @Override
        public boolean readBoolean() throws IOException {
            mReader.nextName();
            return mReader.nextBoolean();
        }

        @Override
        public float readFloat() throws IOException {
            mReader.nextName();
            return Float.valueOf(mReader.nextString());
        }

        @Override
        public int readInt() throws IOException {
            mReader.nextName();
            return mReader.nextInt();
        }
    }

    boolean load(@NonNull LabelCollection labels, File file) {
        boolean loaded = false;
        JsonReader jsonReader = null;
        try {
            InputStream in = new FileInputStream(file);
            jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            loaded = labels.read(new LabelCollectionReader(jsonReader));
        } catch (Exception ignore) {
        } finally {
            if (jsonReader != null) {
                try {
                    jsonReader.close();
                } catch (Exception ignore) {
                }
            }
        }
        return loaded;
    }

    boolean save(@NonNull LabelCollection labels, File file) {
        boolean saved = false;
        JsonWriter jsonWriter = null;
        try {
            OutputStream out = new FileOutputStream(file);
            jsonWriter = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
            jsonWriter.setIndent(" ");
            labels.write(new LabelCollectionWriter(jsonWriter));
            saved = true;
        } catch (Exception ignore) {
        } finally {
            if (jsonWriter != null) {
                try {
                    jsonWriter.close();
                } catch (Exception ignore) {
                }
            }
        }
        return saved;
    }
}
