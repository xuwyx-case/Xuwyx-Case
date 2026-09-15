package com.xuwyx.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
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
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (gameView != null && gameView.goBack()) return;
        super.onBackPressed();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private static final class GameView extends View {
        private static final float VIRTUAL_WIDTH = 1100f;
        private static final float VIRTUAL_HEIGHT = 550f;
        private static final int ITEMS_PER_PAGE = 8;
        private static final int BACKGROUND = Color.rgb(10, 18, 28);
        private static final int PANEL = Color.rgb(37, 61, 88);
        private static final int PANEL_DARK = Color.rgb(17, 31, 48);
        private static final int PANEL_LIGHT = Color.rgb(51, 79, 111);
        private static final int TEXT = Color.rgb(236, 243, 249);
        private static final int MUTED = Color.rgb(152, 174, 195);
        private static final int RED = Color.rgb(238, 39, 49);
        private static final int CYAN = Color.rgb(36, 207, 230);

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

        private static final String[] ACTIVITY_ICONS = {
                "ui_icon_avatars", "ui_icon_equipment", "ui_icon_achievements",
                "ui_icon_statistics", "ui_icon_options"
        };
        private static final String[] ACTIVITY_LABELS = {
                "PROFILE", "INVENTORY", "MISSIONS", "STATS", "SETTINGS"
        };

        private enum Screen { LOADING, MENU, PROFILE, CUSTOMIZE }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final SharedPreferences preferences;
        private final LruCache<String, Bitmap> bitmapCache;
        private final RectF profileButton = new RectF();
        private final RectF bottomProfileButton = new RectF();
        private final RectF firstActivityButton = new RectF();
        private final RectF closeButton = new RectF();
        private final RectF customizeButton = new RectF();
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
        private float viewScale = 1f;
        private float viewOffsetX;
        private float viewOffsetY;

        GameView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            preferences = context.getSharedPreferences("xuwyx_profile", Context.MODE_PRIVATE);
            selectedAvatar = clamp(preferences.getInt("avatar", 0), 0, AVATARS.length - 1);
            selectedFrame = clamp(preferences.getInt("frame", 0), 0, FRAMES.length - 1);
            bitmapCache = new LruCache<String, Bitmap>(36 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return Math.max(1, value.getByteCount() / 1024);
                }
            };
            for (int i = 0; i < itemRects.length; i++) itemRects[i] = new RectF();
            handler.postDelayed(finishLoading, 1750L);
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(finishLoading);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.BLACK);
            viewScale = Math.min(getWidth() / VIRTUAL_WIDTH, getHeight() / VIRTUAL_HEIGHT);
            viewOffsetX = (getWidth() - VIRTUAL_WIDTH * viewScale) * 0.5f;
            viewOffsetY = (getHeight() - VIRTUAL_HEIGHT * viewScale) * 0.5f;
            int save = canvas.save();
            canvas.translate(viewOffsetX, viewOffsetY);
            canvas.scale(viewScale, viewScale);
            if (screen == Screen.LOADING) drawLoading(canvas);
            else if (screen == Screen.MENU) drawMenu(canvas);
            else if (screen == Screen.PROFILE) drawProfile(canvas);
            else drawCustomize(canvas);
            canvas.restoreToCount(save);
        }

        private void drawLoading(Canvas canvas) {
            canvas.drawColor(Color.rgb(7, 43, 60));
            drawBitmapCrop(canvas, bitmap("loading_splash"),
                    new RectF(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
            RectF bar = new RectF(388, 505, 712, 520);
            drawBitmapStretch(canvas, "ui_progress_bg", bar);
            int save = canvas.save();
            canvas.clipRect(bar.left + 2, bar.top + 2,
                    bar.left + bar.width() * .82f, bar.bottom - 2);
            drawBitmapStretch(canvas, "ui_progress", bar);
            canvas.restoreToCount(save);
        }

        private void drawMenu(Canvas canvas) {
            fillGradient(canvas, new RectF(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT),
                    Color.rgb(13, 23, 35), Color.rgb(6, 13, 22), true);
            drawMenuBackdrop(canvas);
            drawTopBar(canvas);
            drawMainPanel(canvas);
            drawQuickButtons(canvas);
            drawBottomBar(canvas);
        }

        private void drawMenuBackdrop(Canvas canvas) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.argb(35, 130, 165, 195));
            for (int offset = -500; offset < 1300; offset += 42) {
                canvas.drawLine(offset, 70, offset + 260, 440, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawTopBar(Canvas canvas) {
            drawBitmapStretch(canvas, "ui_top_bar", new RectF(0, 0, 1100, 70));
            paint.setColor(Color.argb(90, 0, 0, 0));
            canvas.drawRect(0, 68, 1100, 72, paint);
            profileButton.set(12, 5, 286, 66);
            drawAvatar(canvas, new RectF(14, 3, 78, 67),
                    AVATARS[selectedAvatar], FRAMES[selectedFrame]);
            drawText(canvas, "PLAYER", 88, 25, 19, TEXT, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "OPEN PROFILE", 88, 47, 12, MUTED,
                    Paint.Align.LEFT, Typeface.BOLD);

            RectF expBg = new RectF(382, 20, 718, 53);
            drawBitmapStretch(canvas, "ui_progress_bg", expBg);
            int save = canvas.save();
            canvas.clipRect(expBg.left + 3, expBg.top + 4,
                    expBg.left + expBg.width() * .61f, expBg.bottom - 4);
            drawBitmapStretch(canvas, "ui_progress", expBg);
            drawBitmapStretch(canvas, "ui_progress_texture", expBg);
            canvas.restoreToCount(save);
            drawLevelBadge(canvas, new RectF(345, 16, 395, 58), "5");
            drawLevelBadge(canvas, new RectF(705, 16, 755, 58), "6");
            drawText(canvas, "1 240 / 2 000 XP", 550, 36, 13, TEXT,
                    Paint.Align.CENTER, Typeface.BOLD);

            drawBitmapStretch(canvas, "ui_status_panel", new RectF(790, 0, 1100, 70));
            drawText(canvas, "XUWYX CASE", 825, 25, 17, TEXT,
                    Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "ONLINE", 825, 47, 11, CYAN,
                    Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "v2.43.0", 1075, 35, 12, MUTED,
                    Paint.Align.RIGHT, Typeface.NORMAL);
        }

        private void drawLevelBadge(Canvas canvas, RectF rect, String value) {
            fillGradient(canvas, rect, Color.rgb(58, 94, 132), Color.rgb(32, 55, 82), false);
            strokePanel(canvas, rect, Color.rgb(113, 148, 181), 2, 8);
            drawText(canvas, value, rect.centerX(), rect.centerY(), 20, TEXT,
                    Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawMainPanel(Canvas canvas) {
            drawBrandTitle(canvas);
            RectF collectionButton = new RectF(785, 82, 1025, 142);
            drawBitmapStretch(canvas, "ui_shop_button", collectionButton);
            drawBitmapFit(canvas, bitmap("ui_icon_shop"), new RectF(798, 86, 850, 138));
            drawText(canvas, "COLLECTION", 872, 111, 17, TEXT,
                    Paint.Align.LEFT, Typeface.BOLD);

            drawSectionTitle(canvas, "COLLECTIONS", 40, 165);
            RectF viewCollection = new RectF(902, 151, 1000, 205);
            drawBitmapStretch(canvas, "ui_view_button", viewCollection);
            drawText(canvas, "VIEW", viewCollection.centerX(), viewCollection.centerY(),
                    12, TEXT, Paint.Align.CENTER, Typeface.BOLD);

            float cardWidth = 151;
            float gap = 15;
            float left = 40;
            for (int i = 0; i < 5; i++) {
                RectF card = new RectF(left + i * (cardWidth + gap), 188,
                        left + i * (cardWidth + gap) + cardWidth, 308);
                drawCollectionCard(canvas, card, i);
            }

            drawSectionTitle(canvas, "ACTIVITIES", 40, 331);
            RectF viewActivity = new RectF(902, 317, 1000, 371);
            drawBitmapStretch(canvas, "ui_view_button", viewActivity);
            drawText(canvas, "VIEW", viewActivity.centerX(), viewActivity.centerY(),
                    12, TEXT, Paint.Align.CENTER, Typeface.BOLD);
            for (int i = 0; i < ACTIVITY_LABELS.length; i++) {
                RectF card = new RectF(left + i * (cardWidth + gap), 354,
                        left + i * (cardWidth + gap) + cardWidth, 434);
                drawActivityCard(canvas, card, i);
                if (i == 0) firstActivityButton.set(card);
            }
        }

        private void drawBrandTitle(Canvas canvas) {
            drawText(canvas, "XUWYX", 40, 111, 39, TEXT, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "[E]", 178, 111, 39, RED, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "CASE", 252, 111, 39, MUTED, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "MAIN MENU", 42, 139, 12, MUTED,
                    Paint.Align.LEFT, Typeface.BOLD);
        }

        private void drawSectionTitle(Canvas canvas, String label, float x, float y) {
            drawBitmapStretch(canvas, "ui_section_line", new RectF(x, y - 4, x + 850, y + 4));
            roundedPanel(canvas, new RectF(x, y - 15, x + 150, y + 15),
                    Color.rgb(27, 48, 70), 3);
            drawText(canvas, label, x + 12, y, 14, TEXT, Paint.Align.LEFT, Typeface.BOLD);
        }

        private void drawCollectionCard(Canvas canvas, RectF card, int index) {
            roundedPanel(canvas, new RectF(card.left + 2, card.top + 3,
                    card.right + 3, card.bottom + 5), Color.argb(100, 0, 0, 0), 7);
            drawBitmapStretch(canvas, "ui_card", card);
            RectF portrait = new RectF(card.centerX() - 43, card.top + 18,
                    card.centerX() + 43, card.top + 104);
            int avatarIndex = (selectedAvatar + index) % AVATARS.length;
            int frameIndex = index == 1 ? 1 : (selectedFrame + index) % FRAMES.length;
            drawAvatar(canvas, portrait, AVATARS[avatarIndex], FRAMES[frameIndex]);
            RectF nameBar = new RectF(card.left + 7, card.top + 7,
                    card.right - 7, card.top + 30);
            drawBitmapStretch(canvas, "ui_card_name", nameBar);
            drawText(canvas, index == 1 ? "BLUE GEM" : "SET " + (index + 1),
                    card.centerX(), card.top + 18, 11, TEXT, Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawActivityCard(Canvas canvas, RectF card, int index) {
            roundedPanel(canvas, card, Color.rgb(34, 56, 80), 5);
            strokePanel(canvas, card, Color.rgb(67, 97, 128), 1.5f, 5);
            drawBitmapFit(canvas, bitmap(ACTIVITY_ICONS[index]),
                    new RectF(card.centerX() - 28, card.top + 7,
                            card.centerX() + 28, card.top + 55));
            drawText(canvas, ACTIVITY_LABELS[index], card.centerX(), card.bottom - 13,
                    12, TEXT, Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawQuickButtons(Canvas canvas) {
            String[] icons = {"ui_icon_social", "ui_icon_achievements", "ui_icon_boost"};
            for (int i = 0; i < icons.length; i++) {
                RectF button = new RectF(1018, 160 + i * 82, 1090, 232 + i * 82);
                drawBitmapStretch(canvas, "ui_side_button", button);
                drawBitmapFit(canvas, bitmap(icons[i]), inset(button, 17));
            }
        }

        private void drawBottomBar(Canvas canvas) {
            drawBitmapStretch(canvas, "ui_bottom_bar", new RectF(0, 440, 1100, 550));
            paint.setColor(Color.argb(100, 0, 0, 0));
            canvas.drawRect(0, 438, 1100, 442, paint);
            drawBottomItem(canvas, 205, "ui_icon_statistics", "STATS");
            drawBottomItem(canvas, 335, "ui_icon_achievements", "MISSIONS");
            RectF equipment = new RectF(425, 443, 675, 548);
            drawBitmapStretch(canvas, "ui_equipment_button", equipment);
            drawBitmapFit(canvas, bitmap("ui_icon_equipment"), new RectF(494, 450, 606, 508));
            drawText(canvas, "INVENTORY", 550, 527, 14, TEXT,
                    Paint.Align.CENTER, Typeface.BOLD);
            drawBottomItem(canvas, 765, "ui_icon_leaderboards", "RANKING");
            drawBottomItem(canvas, 895, "ui_icon_options", "OPTIONS");
            bottomProfileButton.set(965, 447, 1092, 545);
            drawBitmapStretch(canvas, "ui_social_button", bottomProfileButton);
            drawBitmapFit(canvas, bitmap("ui_icon_avatars"), new RectF(1002, 458, 1056, 510));
            drawText(canvas, "PROFILE", bottomProfileButton.centerX(), 528, 12,
                    TEXT, Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawBottomItem(Canvas canvas, float x, String iconName, String label) {
            drawBitmapFit(canvas, bitmap(iconName), new RectF(x - 24, 454, x + 24, 502));
            drawText(canvas, label, x, 526, 11, MUTED, Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawProfile(Canvas canvas) {
            fillGradient(canvas, new RectF(0, 0, 1100, 550),
                    Color.rgb(10, 20, 31), Color.rgb(4, 10, 17), true);
            drawBitmapStretch(canvas, "ui_profile_panel", new RectF(0, 0, 1100, 550));
            drawText(canvas, "PROFILE", 550, 39, 28, TEXT, Paint.Align.CENTER, Typeface.BOLD);
            closeButton.set(1020, 18, 1082, 80);
            drawCloseButton(canvas, closeButton);

            RectF avatar = new RectF(46, 47, 286, 287);
            drawAvatar(canvas, avatar, AVATARS[selectedAvatar], FRAMES[selectedFrame]);
            drawText(canvas, "PLAYER", 301, 90, 28, TEXT, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "XUWYX #0001", 301, 119, 14, MUTED,
                    Paint.Align.LEFT, Typeface.BOLD);

            RectF bubble = new RectF(300, 137, 648, 262);
            drawBitmapStretch(canvas, "ui_profile_bubble", bubble);
            drawText(canvas, "WELCOME TO XUWYX CASE", 326, 173, 15,
                    Color.rgb(50, 70, 91),
                    Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "Your profile, avatar and frame", 326, 201, 13,
                    Color.rgb(92, 115, 137),
                    Paint.Align.LEFT, Typeface.NORMAL);
            drawText(canvas, "are saved on this device.", 326, 225, 13,
                    Color.rgb(92, 115, 137),
                    Paint.Align.LEFT, Typeface.NORMAL);

            drawProfileInfoRow(canvas, 302, 281, "ui_icon_rank", "LEVEL", "5");
            drawProfileInfoRow(canvas, 302, 321, "ui_icon_country", "REGION", "GLOBAL");
            drawProfileInfoRow(canvas, 302, 361, "ui_icon_cash", "STATUS", "ONLINE");
            drawProfileInfoRow(canvas, 302, 401, "ui_icon_equipment", "LOADOUT", "DEFAULT");
            drawShowcase(canvas, new RectF(682, 114, 1045, 254), "AVATARS", true);
            drawShowcase(canvas, new RectF(682, 277, 1045, 417), "FRAMES", false);

            customizeButton.set(302, 465, 612, 525);
            fillGradient(canvas, customizeButton, Color.rgb(44, 139, 190),
                    Color.rgb(27, 83, 135), false);
            strokePanel(canvas, customizeButton, Color.rgb(95, 190, 226), 2, 4);
            drawBitmapFit(canvas, bitmap("ui_icon_avatars"), new RectF(321, 477, 359, 513));
            drawText(canvas, "EDIT PROFILE", 379, customizeButton.centerY(), 17, TEXT,
                    Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "FRAME: " + friendlyName(FRAMES[selectedFrame]),
                    682, 498, 13, MUTED, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, "AVATAR: " + friendlyName(AVATARS[selectedAvatar]),
                    682, 520, 13, MUTED, Paint.Align.LEFT, Typeface.BOLD);
        }

        private void drawProfileInfoRow(Canvas canvas, float left, float top,
                                        String iconName, String label, String value) {
            RectF row = new RectF(left, top, left + 350, top + 34);
            drawBitmapStretch(canvas, "ui_profile_row_bg", row);
            drawBitmapStretch(canvas, "ui_profile_row_line", row);
            drawBitmapFit(canvas, bitmap(iconName), new RectF(left + 10, top + 7,
                    left + 30, top + 27));
            drawText(canvas, label, left + 42, top + 17, 12, MUTED,
                    Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, value, row.right - 14, top + 17, 13, TEXT,
                    Paint.Align.RIGHT, Typeface.BOLD);
        }

        private void drawShowcase(Canvas canvas, RectF rect, String label, boolean avatars) {
            drawBitmapStretch(canvas, "ui_profile_showcase_bg", rect);
            drawBitmapStretch(canvas, "ui_profile_showcase_line", rect);
            drawBitmapFit(canvas, bitmap(avatars ? "ui_icon_avatars" : "ui_icon_frames"),
                    new RectF(rect.left + 12, rect.top + 10, rect.left + 34, rect.top + 32));
            drawText(canvas, label, rect.left + 43, rect.top + 21, 12, MUTED,
                    Paint.Align.LEFT, Typeface.BOLD);
            for (int i = 0; i < 3; i++) {
                RectF preview = new RectF(rect.left + 28 + i * 106, rect.top + 39,
                        rect.left + 108 + i * 106, rect.top + 119);
                if (avatars) {
                    drawAvatar(canvas, preview, AVATARS[(selectedAvatar + i) % AVATARS.length],
                            FRAMES[selectedFrame]);
                } else {
                    drawAvatar(canvas, preview, AVATARS[selectedAvatar],
                            FRAMES[(selectedFrame + i) % FRAMES.length]);
                }
            }
        }

        private void drawCustomize(Canvas canvas) {
            fillGradient(canvas, new RectF(0, 0, 1100, 550),
                    Color.rgb(11, 22, 34), Color.rgb(5, 11, 18), true);
            drawBitmapStretch(canvas, "ui_selector_panel", new RectF(0, 0, 1100, 550));
            closeButton.set(1031, 18, 1081, 68);
            drawCloseButton(canvas, closeButton);

            avatarTab.set(45, 24, 335, 91);
            frameTab.set(353, 24, 643, 91);
            drawSelectorTab(canvas, avatarTab, "AVATARS", !showingFrames, "ui_icon_avatars");
            drawSelectorTab(canvas, frameTab, "FRAMES", showingFrames, "ui_icon_frames");

            paint.setColor(Color.rgb(41, 67, 97));
            canvas.drawRect(824, 0, 1100, 550, paint);
            drawBitmapStretch(canvas, "ui_separator", new RectF(823, 15, 825, 535));
            RectF preview = new RectF(842, 77, 1082, 317);
            drawAvatar(canvas, preview, AVATARS[selectedAvatar], FRAMES[selectedFrame]);
            drawText(canvas, "PLAYER", 962, 347, 22, TEXT, Paint.Align.CENTER, Typeface.BOLD);
            drawText(canvas, "AVATAR", 866, 385, 12, MUTED, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, friendlyName(AVATARS[selectedAvatar]), 1070, 385,
                    12, TEXT, Paint.Align.RIGHT, Typeface.BOLD);
            drawText(canvas, "FRAME", 866, 416, 12, MUTED, Paint.Align.LEFT, Typeface.BOLD);
            drawText(canvas, friendlyName(FRAMES[selectedFrame]), 1070, 416,
                    12, TEXT, Paint.Align.RIGHT, Typeface.BOLD);
            if (showingFrames && selectedFrame == 1) {
                roundedPanel(canvas, new RectF(865, 443, 1073, 490),
                        Color.rgb(27, 89, 132), 4);
                drawText(canvas, "BLUE GEM  •  9X", 969, 466, 14, CYAN,
                        Paint.Align.CENTER, Typeface.BOLD);
            }

            String[] items = showingFrames ? FRAMES : AVATARS;
            int start = page * ITEMS_PER_PAGE;
            for (int localIndex = 0; localIndex < ITEMS_PER_PAGE; localIndex++) {
                int column = localIndex % 4;
                int row = localIndex / 4;
                float left = 35 + column * 193;
                float top = 109 + row * 187;
                RectF card = itemRects[localIndex];
                card.set(left, top, left + 176, top + 178);
                int itemIndex = start + localIndex;
                if (itemIndex >= items.length) {
                    card.setEmpty();
                    continue;
                }
                boolean selected = showingFrames ? itemIndex == selectedFrame
                        : itemIndex == selectedAvatar;
                drawBitmapStretch(canvas, selected ? "ui_item_selected" : "ui_item_card", card);
                RectF art = new RectF(card.left + 29, card.top + 10,
                        card.right - 29, card.top + 128);
                if (showingFrames) drawAvatar(canvas, art, AVATARS[selectedAvatar], items[itemIndex]);
                else drawAvatar(canvas, art, items[itemIndex], FRAMES[selectedFrame]);
                drawText(canvas, friendlyName(items[itemIndex]), card.centerX(),
                        card.bottom - 29, 12, selected ? TEXT : MUTED,
                        Paint.Align.CENTER, Typeface.BOLD);
                if (showingFrames && "frame_9x".equals(items[itemIndex])) {
                    roundedPanel(canvas, new RectF(card.left + 34, card.top + 8,
                            card.right - 34, card.top + 30), Color.rgb(20, 105, 161), 3);
                    drawText(canvas, "BLUE GEM", card.centerX(), card.top + 19,
                            10, TEXT, Paint.Align.CENTER, Typeface.BOLD);
                }
            }

            int pages = Math.max(1, (items.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
            previousPage.set(280, 493, 350, 538);
            nextPage.set(510, 493, 580, 538);
            drawPageButton(canvas, previousPage, "‹", page > 0);
            drawPageButton(canvas, nextPage, "›", page + 1 < pages);
            drawText(canvas, (page + 1) + " / " + pages, 430, 515, 14, TEXT,
                    Paint.Align.CENTER, Typeface.BOLD);
        }

        private void drawSelectorTab(Canvas canvas, RectF rect, String label,
                                     boolean selected, String iconName) {
            drawBitmapStretch(canvas, "ui_tab", rect);
            if (selected) {
                paint.setColor(RED);
                canvas.drawRect(rect.left + 4, rect.bottom - 5, rect.right - 4, rect.bottom, paint);
            }
            drawBitmapFit(canvas, bitmap(iconName), new RectF(rect.left + 72, rect.top + 20,
                    rect.left + 102, rect.top + 50));
            drawText(canvas, label, rect.left + 116, rect.centerY(), 16,
                    selected ? TEXT : MUTED, Paint.Align.LEFT, Typeface.BOLD);
        }

        private void drawCloseButton(Canvas canvas, RectF rect) {
            drawBitmapFit(canvas, bitmap("ui_close_circle"), rect);
            drawBitmapFit(canvas, bitmap("ui_close_x"), inset(rect, 17));
        }

        private void drawPageButton(Canvas canvas, RectF rect, String label, boolean enabled) {
            roundedPanel(canvas, rect, enabled ? PANEL_LIGHT : PANEL_DARK, 4);
            strokePanel(canvas, rect, enabled ? Color.rgb(102, 138, 171) : PANEL, 1.5f, 4);
            drawText(canvas, label, rect.centerX(), rect.centerY() - 2, 28,
                    enabled ? TEXT : MUTED, Paint.Align.CENTER, Typeface.BOLD);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float x = (event.getX() - viewOffsetX) / viewScale;
            float y = (event.getY() - viewOffsetY) / viewScale;
            if (x < 0 || y < 0 || x > VIRTUAL_WIDTH || y > VIRTUAL_HEIGHT) return true;

            if (screen == Screen.MENU) {
                if (profileButton.contains(x, y) || bottomProfileButton.contains(x, y)
                        || firstActivityButton.contains(x, y)) {
                    screen = Screen.PROFILE;
                    invalidate();
                }
                return true;
            }
            if (screen == Screen.PROFILE) {
                if (closeButton.contains(x, y)) screen = Screen.MENU;
                else if (customizeButton.contains(x, y)) {
                    screen = Screen.CUSTOMIZE;
                    showingFrames = false;
                    page = selectedAvatar / ITEMS_PER_PAGE;
                }
                invalidate();
                return true;
            }
            if (screen != Screen.CUSTOMIZE) return true;
            if (closeButton.contains(x, y)) screen = Screen.PROFILE;
            else if (avatarTab.contains(x, y)) {
                showingFrames = false;
                page = selectedAvatar / ITEMS_PER_PAGE;
            } else if (frameTab.contains(x, y)) {
                showingFrames = true;
                page = selectedFrame / ITEMS_PER_PAGE;
            } else if (previousPage.contains(x, y) && page > 0) page--;
            else {
                String[] items = showingFrames ? FRAMES : AVATARS;
                int pages = Math.max(1, (items.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
                if (nextPage.contains(x, y) && page + 1 < pages) page++;
                else {
                    for (int i = 0; i < itemRects.length; i++) {
                        if (!itemRects[i].isEmpty() && itemRects[i].contains(x, y)) {
                            int itemIndex = page * ITEMS_PER_PAGE + i;
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

        boolean goBack() {
            if (screen == Screen.CUSTOMIZE) {
                screen = Screen.PROFILE;
                invalidate();
                return true;
            }
            if (screen == Screen.PROFILE) {
                screen = Screen.MENU;
                invalidate();
                return true;
            }
            return screen == Screen.LOADING;
        }

        private void drawAvatar(Canvas canvas, RectF destination,
                                String avatarName, String frameName) {
            Bitmap avatar = bitmap(avatarName);
            if (avatar != null) {
                float amount = Math.min(destination.width(), destination.height()) * .105f;
                RectF clipped = inset(destination, amount);
                Path circle = new Path();
                circle.addCircle(clipped.centerX(), clipped.centerY(),
                        Math.min(clipped.width(), clipped.height()) * .5f, Path.Direction.CW);
                int save = canvas.save();
                canvas.clipPath(circle);
                drawBitmapCrop(canvas, avatar, clipped);
                canvas.restoreToCount(save);
            }
            drawBitmapFit(canvas, bitmap(frameName), destination);
        }

        private Bitmap bitmap(String resourceName) {
            Bitmap cached = bitmapCache.get(resourceName);
            if (cached != null) return cached;
            int resourceId = getResources().getIdentifier(resourceName, "drawable",
                    getContext().getPackageName());
            if (resourceId == 0) return null;
            Bitmap decoded = BitmapFactory.decodeResource(getResources(), resourceId);
            if (decoded != null) bitmapCache.put(resourceName, decoded);
            return decoded;
        }

        private void drawBitmapStretch(Canvas canvas, String name, RectF destination) {
            Bitmap source = bitmap(name);
            if (source != null) {
                paint.setShader(null);
                paint.setAlpha(255);
                canvas.drawBitmap(source, null, destination, paint);
            }
        }

        private void drawBitmapFit(Canvas canvas, Bitmap source, RectF destination) {
            if (source == null) return;
            float ratio = Math.min(destination.width() / source.getWidth(),
                    destination.height() / source.getHeight());
            float width = source.getWidth() * ratio;
            float height = source.getHeight() * ratio;
            RectF target = new RectF(destination.centerX() - width * .5f,
                    destination.centerY() - height * .5f,
                    destination.centerX() + width * .5f,
                    destination.centerY() + height * .5f);
            paint.setShader(null);
            paint.setAlpha(255);
            canvas.drawBitmap(source, null, target, paint);
        }

        private void drawBitmapCrop(Canvas canvas, Bitmap source, RectF destination) {
            if (source == null || destination.width() <= 0 || destination.height() <= 0) return;
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
            paint.setShader(null);
            paint.setAlpha(255);
            canvas.drawBitmap(source, sourceRect, destination, paint);
        }

        private void fillGradient(Canvas canvas, RectF rect, int start, int end, boolean vertical) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(rect.left, rect.top,
                    vertical ? rect.left : rect.right, vertical ? rect.bottom : rect.top,
                    start, end, Shader.TileMode.CLAMP));
            canvas.drawRect(rect, paint);
            paint.setShader(null);
        }

        private void roundedPanel(Canvas canvas, RectF rect, int color, float radius) {
            paint.setShader(null);
            paint.setAlpha(255);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        private void strokePanel(Canvas canvas, RectF rect, int color, float width, float radius) {
            paint.setShader(null);
            paint.setAlpha(255);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);
            paint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawText(Canvas canvas, String text, float x, float centerY,
                              float size, int color, Paint.Align align, int style) {
            paint.setShader(null);
            paint.setAlpha(255);
            paint.setTextAlign(align);
            paint.setTypeface(Typeface.create("sans-serif-condensed", style));
            paint.setTextSize(size);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            canvas.drawText(text, x, centerY - (metrics.ascent + metrics.descent) * .5f, paint);
        }

        private static RectF inset(RectF rect, float amount) {
            return new RectF(rect.left + amount, rect.top + amount,
                    rect.right - amount, rect.bottom - amount);
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private static String friendlyName(String resourceName) {
            if ("frame_9x".equals(resourceName)) return "BLUE GEM";
            if ("frame_defaultframe".equals(resourceName)
                    || "avatar_defaultavatar".equals(resourceName)) return "DEFAULT";
            int separator = resourceName.indexOf('_');
            return (separator >= 0 ? resourceName.substring(separator + 1) : resourceName)
                    .toUpperCase();
        }
    }
}
