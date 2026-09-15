package com.xuwyx.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        hideSystemUi();
        setContentView(new MainMenuView(this));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private static final class MainMenuView extends View {
        private static final int BACKGROUND = Color.rgb(5, 15, 31);
        private static final int LOWER_BACKGROUND = Color.rgb(8, 30, 52);
        private static final int RED = Color.rgb(242, 15, 25);
        private static final int TITLE = Color.rgb(238, 245, 255);
        private static final int MUTED = Color.rgb(91, 130, 168);

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final RectF rectangle = new RectF();

        MainMenuView(Context context) {
            super(context);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            setBackgroundColor(BACKGROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();
            float scale = Math.min(width / 1920f, height / 1080f);

            canvas.drawColor(BACKGROUND);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(LOWER_BACKGROUND);
            rectangle.set(0f, height * 0.67f, width, height);
            canvas.drawRect(rectangle, paint);

            paint.setColor(Color.argb(70, 242, 15, 25));
            rectangle.set(width * 0.24f, height * 0.5f - 3f * scale,
                    width * 0.76f, height * 0.5f + 3f * scale);
            canvas.drawRoundRect(rectangle, 3f * scale, 3f * scale, paint);

            drawCenteredText(canvas, "[E]", height * 0.36f, 210f * scale,
                    RED, Typeface.BOLD);
            drawCenteredText(canvas, "XUWYX CASE", height * 0.61f, 66f * scale,
                    TITLE, Typeface.BOLD);
            drawCenteredText(canvas, "MAIN MENU", height * 0.72f, 25f * scale,
                    MUTED, Typeface.NORMAL);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            paint.setTextSize(20f * scale);
            paint.setColor(MUTED);
            canvas.drawText("VERSION 2.43.0", width - 28f * scale,
                    height - 28f * scale, paint);
        }

        private void drawCenteredText(Canvas canvas, String text, float centerY,
                                      float textSize, int color, int style) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif", style));
            paint.setTextSize(textSize);
            paint.setColor(color);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(text, getWidth() / 2f, baseline, paint);
        }
    }
}

