package com.xuwyx.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.MotionEvent;
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
        setContentView(new GameView(this));
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

    private static final class GameView extends View {
        private static final int BACKGROUND = Color.rgb(7, 20, 38);
        private static final int PANEL = Color.rgb(17, 40, 66);
        private static final int PANEL_DARK = Color.rgb(8, 25, 46);
        private static final int RED = Color.rgb(232, 32, 39);
        private static final int BLUE = Color.rgb(72, 132, 185);
        private static final int TEXT = Color.rgb(239, 246, 255);
        private static final int MUTED = Color.rgb(128, 160, 190);
        private static final int ITEMS_PER_PAGE = 12;

        private static final String[] AVATARS = {
                "avatar_defaultavatar", "avatar_0o", "avatar_2w", "avatar_5t_0",
                "avatar_5x", "avatar_as", "avatar_c2", "avatar_g5",
                "avatar_hv", "avatar_in", "avatar_ix", "avatar_l8",
                "avatar_po_0", "avatar_qt", "avatar_sp", "avatar_t7",
                "avatar_v3", "avatar_wc", "avatar_xx"
        };

        private static final String[] FRAMES = {
                "frame_defaultframe", "frame_9x", "frame_0l", "frame_0n",
                "frame_1b", "frame_3e", "frame_3f", "frame_3x",
                "frame_4f", "frame_4v", "frame_5g", "frame_5t",
                "frame_66", "frame_6b", "frame_6r", "frame_7u",
                "frame_80", "frame_8a", "frame_8z", "frame_9f",
                "frame_9i", "frame_a1", "frame_ac", "frame_aj",
                "frame_c0", "frame_ek", "frame_fg", "frame_gb",
                "frame_hg", "frame_ho", "frame_ij", "frame_jk",
                "frame_jw", "frame_kl", "frame_lk", "frame_lp",
                "frame_po", "frame_qo", "frame_s1", "frame_t5",
                "frame_v7", "frame_vg", "frame_xd", "frame_y6"
        };

        private enum Screen {
            LOADING,
            MENU,
            PROFILE
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final SharedPreferences preferences;
        private final LruCache<String, Bitmap> bitmapCache;
        private final RectF profileButton = new RectF();
        private final RectF backButton = new RectF();
        private final RectF avatarTab = new RectF();
        private final RectF frameTab = new RectF();
        private final RectF previousPage = new RectF();
        private final RectF nextPage = new RectF();
        private final RectF[] itemRects = new RectF[ITEMS_PER_PAGE];
        private final Runnable finishLoading = () -> {
            screen = Screen.MENU;
            invalidate();
        };

        private Screen screen = Screen.LOADING;
        private boolean showingFrames;
        private int selectedAvatar;
        private int selectedFrame;
        private int page;

        GameView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            preferences = context.getSharedPreferences("xuwyx_profile", Context.MODE_PRIVATE);
            selectedAvatar = clamp(preferences.getInt("avatar", 0), 0, AVATARS.length - 1);
            selectedFrame = clamp(preferences.getInt("frame", 0), 0, FRAMES.length - 1);

            bitmapCache = new LruCache<String, Bitmap>(24 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return Math.max(1, value.getByteCount() / 1024);
                }
            };

            for (int index = 0; index < itemRects.length; index++) {
                itemRects[index] = new RectF();
            }

            handler.postDelayed(finishLoading, 1600L);
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(finishLoading);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (screen == Screen.LOADING) {
                drawLoading(canvas);
            } else if (screen == Screen.MENU) {
                drawMenu(canvas);
            } else {
                drawProfile(canvas);
            }
        }

        private void drawLoading(Canvas canvas) {
            canvas.drawColor(Color.rgb(8, 46, 65));
            Bitmap splash = bitmap("loading_splash");
            if (splash != null) {
                drawBitmapFit(canvas, splash, new RectF(0f, 0f, getWidth(), getHeight()));
            }

            float scale = scale();
            float barWidth = getWidth() * 0.28f;
            float left = (getWidth() - barWidth) * 0.5f;
            float top = getHeight() - 92f * scale;
            roundedPanel(canvas, new RectF(left, top, left + barWidth, top + 12f * scale),
                    Color.argb(150, 4, 18, 31), 8f * scale);
            roundedPanel(canvas, new RectF(left, top, left + barWidth * 0.78f, top + 12f * scale),
                    RED, 8f * scale);
            drawText(canvas, "LOADING", getWidth() * 0.5f, top - 18f * scale,
                    22f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawMenu(Canvas canvas) {
            canvas.drawColor(BACKGROUND);
            float width = getWidth();
            float height = getHeight();
            float scale = scale();

            drawBitmapStretch(canvas, "menu_newmenu_bg_upper_panel",
                    new RectF(0f, 0f, width, 78f * scale));
            drawBitmapStretch(canvas, "menu_newmenu_bg_bottom_panel",
                    new RectF(0f, height - 126f * scale, width, height));

            drawText(canvas, "XUWYX CASE", 34f * scale, 52f * scale,
                    34f * scale, TEXT, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "2.43.0", width - 30f * scale, 50f * scale,
                    20f * scale, MUTED, Paint.Align.RIGHT, Typeface.NORMAL);

            profileButton.set(36f * scale, 112f * scale, 300f * scale, 420f * scale);
            roundedPanel(canvas, profileButton, PANEL, 20f * scale);
            RectF profileAvatar = new RectF(profileButton.left + 42f * scale,
                    profileButton.top + 26f * scale,
                    profileButton.right - 42f * scale,
                    profileButton.top + 242f * scale);
            drawAvatar(canvas, profileAvatar, AVATARS[selectedAvatar], FRAMES[selectedFrame]);
            drawText(canvas, "PLAYER", profileButton.centerX(), profileButton.bottom - 38f * scale,
                    26f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);
            drawText(canvas, "PROFILE", profileButton.centerX(), profileButton.bottom - 12f * scale,
                    16f * scale, MUTED, Paint.Align.CENTER, Typeface.BOLD);

            RectF casePanel = new RectF(width * 0.31f, height * 0.18f,
                    width * 0.69f, height * 0.79f);
            drawBitmapStretch(canvas, "menu_newmenu_bg_case", casePanel);
            drawText(canvas, "[E]", casePanel.centerX(), casePanel.top + casePanel.height() * 0.29f,
                    104f * scale, RED, Paint.Align.CENTER, Typeface.BOLD);
            drawText(canvas, "XUWYX CASE", casePanel.centerX(), casePanel.top + casePanel.height() * 0.52f,
                    44f * scale, Color.rgb(20, 39, 58), Paint.Align.CENTER, Typeface.BOLD);
            RectF playButton = new RectF(casePanel.left + 54f * scale,
                    casePanel.bottom - 112f * scale,
                    casePanel.right - 54f * scale,
                    casePanel.bottom - 40f * scale);
            drawBitmapStretch(canvas, "menu_newmenu_goldbuton", playButton);
            drawText(canvas, "PLAY", playButton.centerX(), playButton.centerY(),
                    28f * scale, Color.rgb(31, 42, 53), Paint.Align.CENTER, Typeface.BOLD);

            RectF equipment = new RectF(width - 305f * scale, 120f * scale,
                    width - 42f * scale, 380f * scale);
            drawBitmapStretch(canvas, "menu_newmenu_button_eq", equipment);
            drawBitmapFit(canvas, bitmap("menu_newmenu_eq_icon"),
                    inset(equipment, 62f * scale));
            drawText(canvas, "EQUIPMENT", equipment.centerX(), equipment.bottom + 34f * scale,
                    19f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);

            drawText(canvas, "Tap PROFILE to open your player card",
                    width * 0.5f, height - 54f * scale,
                    22f * scale, TEXT, Paint.Align.CENTER, Typeface.NORMAL);
        }

        private void drawProfile(Canvas canvas) {
            canvas.drawColor(BACKGROUND);
            float width = getWidth();
            float height = getHeight();
            float scale = scale();

            drawBitmapStretch(canvas, "profile_global_quest_profile_bg",
                    new RectF(0f, 0f, width, 112f * scale));
            drawText(canvas, "PROFILE", width * 0.5f, 61f * scale,
                    38f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);

            backButton.set(24f * scale, 20f * scale, 178f * scale, 92f * scale);
            roundedPanel(canvas, backButton, Color.argb(190, 6, 22, 40), 12f * scale);
            drawText(canvas, "‹  BACK", backButton.centerX(), backButton.centerY(),
                    22f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);

            RectF identityPanel = new RectF(30f * scale, 138f * scale,
                    550f * scale, height - 34f * scale);
            roundedPanel(canvas, identityPanel, PANEL_DARK, 22f * scale);
            drawBitmapStretch(canvas, "profile_profile_frame2_bg", identityPanel);

            RectF largeAvatar = new RectF(identityPanel.left + 110f * scale,
                    identityPanel.top + 36f * scale,
                    identityPanel.right - 110f * scale,
                    identityPanel.top + 366f * scale);
            drawAvatar(canvas, largeAvatar, AVATARS[selectedAvatar], FRAMES[selectedFrame]);
            drawText(canvas, "PLAYER", identityPanel.centerX(), identityPanel.top + 424f * scale,
                    34f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);
            drawText(canvas, "XUWYX #0001", identityPanel.centerX(), identityPanel.top + 466f * scale,
                    21f * scale, MUTED, Paint.Align.CENTER, Typeface.NORMAL);
            drawText(canvas, "Selected frame: " + friendlyName(FRAMES[selectedFrame]),
                    identityPanel.centerX(), identityPanel.bottom - 46f * scale,
                    18f * scale, TEXT, Paint.Align.CENTER, Typeface.NORMAL);

            float contentLeft = identityPanel.right + 34f * scale;
            avatarTab.set(contentLeft, 138f * scale, contentLeft + 265f * scale, 205f * scale);
            frameTab.set(avatarTab.right + 18f * scale, avatarTab.top,
                    avatarTab.right + 283f * scale, avatarTab.bottom);
            drawTab(canvas, avatarTab, "AVATARS", !showingFrames);
            drawTab(canvas, frameTab, "FRAMES", showingFrames);

            String[] items = showingFrames ? FRAMES : AVATARS;
            int start = page * ITEMS_PER_PAGE;
            float gridTop = 236f * scale;
            float gap = 15f * scale;
            float availableWidth = width - contentLeft - 34f * scale;
            float slotWidth = (availableWidth - gap * 5f) / 6f;
            float slotHeight = Math.min(240f * scale,
                    (height - gridTop - 112f * scale - gap) / 2f);

            for (int localIndex = 0; localIndex < ITEMS_PER_PAGE; localIndex++) {
                int column = localIndex % 6;
                int row = localIndex / 6;
                RectF slot = itemRects[localIndex];
                float left = contentLeft + column * (slotWidth + gap);
                float top = gridTop + row * (slotHeight + gap);
                slot.set(left, top, left + slotWidth, top + slotHeight);

                int itemIndex = start + localIndex;
                if (itemIndex >= items.length) {
                    slot.setEmpty();
                    continue;
                }

                boolean selected = showingFrames
                        ? itemIndex == selectedFrame
                        : itemIndex == selectedAvatar;
                roundedPanel(canvas, slot, selected ? Color.rgb(42, 81, 116) : PANEL,
                        14f * scale);
                if (selected) {
                    strokePanel(canvas, slot, RED, 4f * scale, 14f * scale);
                }

                RectF art = new RectF(slot.left + 12f * scale, slot.top + 10f * scale,
                        slot.right - 12f * scale, slot.bottom - 38f * scale);
                if (showingFrames) {
                    drawBitmapFit(canvas, bitmap(items[itemIndex]), art);
                } else {
                    drawRoundAvatar(canvas, art, items[itemIndex]);
                }
                drawText(canvas, friendlyName(items[itemIndex]), slot.centerX(),
                        slot.bottom - 17f * scale, 14f * scale,
                        selected ? TEXT : MUTED, Paint.Align.CENTER, Typeface.BOLD);
            }

            previousPage.set(width - 330f * scale, height - 80f * scale,
                    width - 220f * scale, height - 24f * scale);
            nextPage.set(width - 132f * scale, height - 80f * scale,
                    width - 22f * scale, height - 24f * scale);
            int pages = Math.max(1, (items.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
            drawPageButton(canvas, previousPage, "‹", page > 0);
            drawPageButton(canvas, nextPage, "›", page + 1 < pages);
            drawText(canvas, (page + 1) + " / " + pages,
                    width - 176f * scale, height - 51f * scale,
                    18f * scale, TEXT, Paint.Align.CENTER, Typeface.BOLD);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }

            float x = event.getX();
            float y = event.getY();
            if (screen == Screen.MENU && profileButton.contains(x, y)) {
                screen = Screen.PROFILE;
                invalidate();
                return true;
            }

            if (screen != Screen.PROFILE) {
                return true;
            }

            if (backButton.contains(x, y)) {
                screen = Screen.MENU;
            } else if (avatarTab.contains(x, y)) {
                showingFrames = false;
                page = selectedAvatar / ITEMS_PER_PAGE;
            } else if (frameTab.contains(x, y)) {
                showingFrames = true;
                page = selectedFrame / ITEMS_PER_PAGE;
            } else if (previousPage.contains(x, y) && page > 0) {
                page--;
            } else {
                String[] items = showingFrames ? FRAMES : AVATARS;
                int pages = Math.max(1, (items.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
                if (nextPage.contains(x, y) && page + 1 < pages) {
                    page++;
                } else {
                    for (int localIndex = 0; localIndex < itemRects.length; localIndex++) {
                        if (!itemRects[localIndex].isEmpty()
                                && itemRects[localIndex].contains(x, y)) {
                            int itemIndex = page * ITEMS_PER_PAGE + localIndex;
                            if (itemIndex < items.length) {
                                if (showingFrames) {
                                    selectedFrame = itemIndex;
                                    preferences.edit().putInt("frame", selectedFrame).apply();
                                } else {
                                    selectedAvatar = itemIndex;
                                    preferences.edit().putInt("avatar", selectedAvatar).apply();
                                }
                            }
                            break;
                        }
                    }
                }
            }

            invalidate();
            return true;
        }

        private void drawAvatar(Canvas canvas, RectF destination,
                                String avatarName, String frameName) {
            drawRoundAvatar(canvas, destination, avatarName);
            drawBitmapFit(canvas, bitmap(frameName), destination);
        }

        private void drawRoundAvatar(Canvas canvas, RectF destination, String avatarName) {
            Bitmap avatar = bitmap(avatarName);
            if (avatar == null) {
                return;
            }
            float inset = Math.min(destination.width(), destination.height()) * 0.105f;
            RectF clipped = inset(destination, inset);
            Path circle = new Path();
            circle.addCircle(clipped.centerX(), clipped.centerY(),
                    Math.min(clipped.width(), clipped.height()) * 0.5f, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(circle);
            drawBitmapCrop(canvas, avatar, clipped);
            canvas.restoreToCount(save);
        }

        private void drawTab(Canvas canvas, RectF rect, String label, boolean selected) {
            roundedPanel(canvas, rect, selected ? RED : PANEL, 12f * scale());
            drawText(canvas, label, rect.centerX(), rect.centerY(),
                    20f * scale(), TEXT, Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawPageButton(Canvas canvas, RectF rect, String label, boolean enabled) {
            roundedPanel(canvas, rect, enabled ? BLUE : PANEL_DARK, 10f * scale());
            drawText(canvas, label, rect.centerX(), rect.centerY() - 2f * scale(),
                    38f * scale(), enabled ? TEXT : MUTED,
                    Paint.Align.CENTER, Typeface.BOLD);
        }

        private Bitmap bitmap(String resourceName) {
            Bitmap cached = bitmapCache.get(resourceName);
            if (cached != null) {
                return cached;
            }
            int resourceId = getResources().getIdentifier(
                    resourceName, "drawable", getContext().getPackageName());
            if (resourceId == 0) {
                return null;
            }
            Bitmap decoded = BitmapFactory.decodeResource(getResources(), resourceId);
            if (decoded != null) {
                bitmapCache.put(resourceName, decoded);
            }
            return decoded;
        }

        private void drawBitmapStretch(Canvas canvas, String resourceName, RectF destination) {
            Bitmap source = bitmap(resourceName);
            if (source != null) {
                canvas.drawBitmap(source, null, destination, paint);
            }
        }

        private void drawBitmapFit(Canvas canvas, Bitmap source, RectF destination) {
            if (source == null) {
                return;
            }
            float ratio = Math.min(destination.width() / source.getWidth(),
                    destination.height() / source.getHeight());
            float width = source.getWidth() * ratio;
            float height = source.getHeight() * ratio;
            RectF target = new RectF(destination.centerX() - width * 0.5f,
                    destination.centerY() - height * 0.5f,
                    destination.centerX() + width * 0.5f,
                    destination.centerY() + height * 0.5f);
            canvas.drawBitmap(source, null, target, paint);
        }

        private void drawBitmapCrop(Canvas canvas, Bitmap source, RectF destination) {
            float sourceRatio = (float) source.getWidth() / source.getHeight();
            float destinationRatio = destination.width() / destination.height();
            Rect sourceRect;
            if (sourceRatio > destinationRatio) {
                int cropWidth = Math.round(source.getHeight() * destinationRatio);
                int left = (source.getWidth() - cropWidth) / 2;
                sourceRect = new Rect(left, 0, left + cropWidth, source.getHeight());
            } else {
                int cropHeight = Math.round(source.getWidth() / destinationRatio);
                int top = (source.getHeight() - cropHeight) / 2;
                sourceRect = new Rect(0, top, source.getWidth(), top + cropHeight);
            }
            canvas.drawBitmap(source, sourceRect, destination, paint);
        }

        private void roundedPanel(Canvas canvas, RectF rect, int color, float radius) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        private void strokePanel(Canvas canvas, RectF rect, int color, float width, float radius) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);
            paint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawText(Canvas canvas, String text, float x, float centerY,
                              float textSize, int color, Paint.Align align, int style) {
            paint.setTextAlign(align);
            paint.setTypeface(Typeface.create("sans-serif", style));
            paint.setTextSize(textSize);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = centerY - (metrics.ascent + metrics.descent) * 0.5f;
            canvas.drawText(text, x, baseline, paint);
        }

        private float scale() {
            return Math.min(getWidth() / 1920f, getHeight() / 1080f);
        }

        private static RectF inset(RectF rect, float amount) {
            return new RectF(rect.left + amount, rect.top + amount,
                    rect.right - amount, rect.bottom - amount);
        }

        private static int clamp(int value, int minimum, int maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }

        private static String friendlyName(String resourceName) {
            if ("frame_9x".equals(resourceName)) {
                return "BLUE GEM";
            }
            if ("frame_defaultframe".equals(resourceName)
                    || "avatar_defaultavatar".equals(resourceName)) {
                return "DEFAULT";
            }
            int separator = resourceName.indexOf('_');
            return (separator >= 0 ? resourceName.substring(separator + 1) : resourceName)
                    .toUpperCase();
        }
    }
}
