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

import androidx.annotation.NonNull;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class FontFamilySet {
    class FontFamily {
        String name;
        String displayName;
        boolean bold;
        boolean italic;
    }

    private final List<FontFamily> mFamilySet;
    private final Context mContext;

    FontFamilySet(Context context) {
        mContext = context;
        mFamilySet = new ArrayList<>();
        fillWithSystemFonts(mFamilySet);
        if (mFamilySet.size() == 0)
            mFamilySet.add(getDefaultFontFamily());
    }

    @NonNull
    private FontFamily getDefaultFontFamily() {
        FontFamily defaultFontFamily = new FontFamily();
        defaultFontFamily.name = null;
        defaultFontFamily.displayName = mContext.getString(R.string.font_default);
        defaultFontFamily.bold = true;
        defaultFontFamily.italic = true;
        return defaultFontFamily;
    }

    @NonNull
    FontFamily getFontFamily(String name) {
        if (name != null) {
            for (FontFamily fontFamily : mFamilySet) {
                if (name.equals(fontFamily.name))
                    return fontFamily;
            }
        }
        return mFamilySet.get(0);
    }

    @NonNull
    FontFamily getFontFamilyFromDisplayName(@NonNull String displayName) {
        for (FontFamily fontFamily : mFamilySet) {
            if (displayName.equals(fontFamily.displayName))
                return fontFamily;
        }
        return mFamilySet.get(0);
    }

    @NonNull
    List<String> getFontFamilyDisplayNameList() {
        List<String> names = new ArrayList<>();
        for (FontFamily fontFamily : mFamilySet)
            names.add(fontFamily.displayName);
        return names;
    }

    private void fillWithSystemFonts(@NonNull List<FontFamily> familySet) {
        File fontsFile = new File("/system/etc/system_fonts.xml");
        if (!fontsFile.exists()) {
            fontsFile = new File("/system/etc/fonts.xml");
            if (!fontsFile.exists())
                return;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(fontsFile);
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.next();
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("familyset"))
                readFamilySet(parser, familySet);
        } catch (Exception ignore) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private void readFamilySet(@NonNull XmlPullParser parser, @NonNull List<FontFamily> familySet)
            throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("family")) {
                FontFamily fontFamily = readFamily(parser);
                if (fontFamily.displayName != null)
                    familySet.add(fontFamily);
            }
        }
    }

    @NonNull
    private FontFamily readFamily(@NonNull XmlPullParser parser)
            throws XmlPullParserException, IOException {
        FontFamily fontFamily = new FontFamily();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "nameset":
                        readNameSet(parser, fontFamily);
                        break;
                    case "fileset":
                        readFileSet(parser, fontFamily);
                        break;
                }
            }
        }
        return fontFamily;
    }

    private void readNameSet(@NonNull XmlPullParser parser, @NonNull FontFamily fontFamily)
            throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("name")) {
                if (fontFamily.name == null)
                    fontFamily.name = readText(parser);
                else {
                    // skip all other names
                    parser.next();
                    parser.next();
                }
            }
        }
    }

    private void readFileSet(@NonNull XmlPullParser parser, @NonNull FontFamily fontFamily)
            throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("file"))
                parseDisplayNameAndStyle(readText(parser), fontFamily);
        }
    }

    private void parseDisplayNameAndStyle(String fontFileName, @NonNull FontFamily fontFamily) {
        // Example: RobotoCondensed-LightItalic.ttf
        // { "RobotoCondensed", "LightItalic" }
        String[] familyInfo = fontFileName.split("\\.")[0].split("-");
        String s = "";
        if (familyInfo.length > 1) {
            s = familyInfo[1];
            if (s.contains("Bold"))
                fontFamily.bold = true;
            if (s.contains("Italic"))
                fontFamily.italic = true;
        }
        if (fontFamily.displayName == null) {
            // "Light"
            s = s.replace("Regular", "").replace("Bold", "").replace("Italic", "");
            // "Roboto Condensed Light"
            fontFamily.displayName = (familyInfo[0] + s).replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
        }
    }

    private String readText(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.next();
        }
        return text;
    }
}
