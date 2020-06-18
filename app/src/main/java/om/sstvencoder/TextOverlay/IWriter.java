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
package om.sstvencoder.TextOverlay;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface IWriter {
    void beginRootObject() throws IOException;

    void beginObject(@NonNull String name) throws IOException;

    void endObject() throws IOException;

    void beginArray(@NonNull String name) throws IOException;

    void endArray() throws IOException;

    void write(@NonNull String name, String value) throws IOException;

    void write(@NonNull String name, boolean value) throws IOException;

    void write(@NonNull String name, float value) throws IOException;

    void write(@NonNull String name, int value) throws IOException;
}
