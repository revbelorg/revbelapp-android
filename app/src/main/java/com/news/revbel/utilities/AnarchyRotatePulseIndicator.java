package com.news.revbel.utilities;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;

import com.news.revbel.R;
import com.news.revbel.RevApplication;
import com.wang.avi.Indicator;

import java.util.ArrayList;

public class AnarchyRotatePulseIndicator extends Indicator {
    private float scaleFloat1, scaleFloat2, degrees;
    private Bitmap bitmap;

    @Override
    public void draw(Canvas canvas, Paint paint) {
        float circleSpacing=12;
        float x = getWidth()/2;
        float y = getHeight()/2;

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(scaleFloat2, scaleFloat2);
        canvas.rotate(degrees);
        paint.setStyle(Paint.Style.FILL);

        int usedColor = paint.getColor();

        Context context = RevApplication.getInstance();
        int newColor = ContextCompat.getColor(context, R.color.white_halftransparent);
        paint.setColor(newColor);
        canvas.drawCircle( 0, 0, x , paint);

        paint.setColor(usedColor);

        canvas.restore();
        canvas.save();

        canvas.translate(x, y);
        canvas.scale(scaleFloat1, scaleFloat1);

        if (bitmap != null) {
            float offset = (x / 2);
            Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            canvas.drawBitmap(bitmap, srcRect, new RectF(-offset, -offset, offset, offset), paint);
        }

        canvas.restore();
        canvas.translate(x, y);
        canvas.scale(scaleFloat2, scaleFloat2);
        canvas.rotate(degrees);

        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);

        //draw two arc
        float[] startAngles=new float[]{225,45};
        for (int i = 0; i < 2; i++) {
            RectF rectF = new RectF(-x+circleSpacing,-y+circleSpacing,x-circleSpacing,y-circleSpacing);
            canvas.drawArc(rectF, startAngles[i], 90, false, paint);
        }
    }

    @Override
    public ArrayList<ValueAnimator> onCreateAnimators() {
        Context context = RevApplication.getInstance();


        Drawable drawable = VectorDrawableCompat.create(context.getResources(), R.drawable.anarchy_symbol, null);
//        ContextCompat.getDrawable(context, R.drawable.anarchy_symbol);
        bitmap = getBitmap(drawable);

        ValueAnimator scaleAnim=ValueAnimator.ofFloat(1,0.3f,1);
        scaleAnim.setDuration(1000);
        scaleAnim.setRepeatCount(-1);
        addUpdateListener(scaleAnim, animation -> {
            scaleFloat1 = (float) animation.getAnimatedValue();
            postInvalidate();
        });

        ValueAnimator scaleAnim2=ValueAnimator.ofFloat(1,0.6f,1);
        scaleAnim2.setDuration(1000);
        scaleAnim2.setRepeatCount(-1);
        addUpdateListener(scaleAnim2, animation -> {
            scaleFloat2 = (float) animation.getAnimatedValue();
            postInvalidate();
        });

        ValueAnimator rotateAnim= ValueAnimator.ofFloat(0, 180,360);
        rotateAnim.setDuration(1000);
        rotateAnim.setRepeatCount(-1);
        addUpdateListener(rotateAnim, animation -> {
            degrees = (float) animation.getAnimatedValue();
            postInvalidate();
        });
        ArrayList<ValueAnimator> animators=new ArrayList<>();
        animators.add(scaleAnim);
        animators.add(scaleAnim2);
        animators.add(rotateAnim);
        return animators;
    }

    private static Bitmap getBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(64,
                64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
