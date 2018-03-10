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

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import om.sstvencoder.ColorPalette.ColorPaletteView;

public class ColorFragment extends DialogFragment
        implements ColorPaletteView.OnColorSelectedListener {

    public interface OnColorSelectedListener {
        void onColorSelected(DialogFragment fragment, int color);

        void onCancel(DialogFragment fragment);
    }

    private List<OnColorSelectedListener> mListeners;
    private int mTitle;
    private int mColor;

    public ColorFragment() {
        mListeners = new ArrayList<>();
        mTitle = R.string.color;
        mColor = Color.WHITE;
    }

    public void setTitle(int title) {
        mTitle = title;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public void addOnColorSelectedListener(OnColorSelectedListener listener) {
        mListeners.add(listener);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_color, null);
        ColorPaletteView colorView = view.findViewById(R.id.select_color);
        colorView.setColor(mColor);
        colorView.addOnColorSelectedListener(this);
        builder.setTitle(mTitle);
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onColorChanged(View v, int color) {
    }

    @Override
    public void onColorSelected(View v, int color) {
        for (OnColorSelectedListener listener : mListeners)
            listener.onColorSelected(this, color);
        dismiss();
    }

    @Override
    public void onCancel(View v) {
        for (OnColorSelectedListener listener : mListeners)
            listener.onCancel(this);
        dismiss();
    }
}
