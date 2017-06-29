package com.news.revbel.utilities;

import android.content.Context;
import android.util.AttributeSet;

import io.github.mthli.knife.KnifeText;

public class RevKnifeText extends KnifeText {
    private boolean italicEnter, boldEnter;
    public RevKnifeText(Context context) {
        super(context);
    }

    public RevKnifeText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RevKnifeText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RevKnifeText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        if (boldEnter) styleValid(1, start, start + count);
        if (italicEnter) styleValid(2, start, start + count);
    }

    public boolean isBoldCursor() {
        return boldEnter;
    }

    public boolean isItalicCursor() {
        return italicEnter;
    }


    public void toggleBold() {
        boldEnter = !boldEnter;
    }

    public void toggleItalic() {
        italicEnter = !italicEnter;
    }
}
