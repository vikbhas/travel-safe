package com.isee_project.travelsafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.isee_project.travelsafe.R;

import static android.content.Context.MODE_PRIVATE;

public class CustomAlterText extends androidx.appcompat.widget.AppCompatEditText {

    public CustomAlterText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == EditorInfo.IME_ACTION_DONE) {

            InputMethodManager mgr = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);

            SharedPreferences.Editor editor = getContext().getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
            editor.apply();

            clearFocus();

            Toast.makeText(getContext(), R.string.profile_permissions_locationUpdateFrequency, Toast.LENGTH_LONG).show();
        }
        return true;
    }
}
