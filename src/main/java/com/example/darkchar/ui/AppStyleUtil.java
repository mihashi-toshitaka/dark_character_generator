package com.example.darkchar.ui;

import javafx.scene.control.Alert;

/**
 * アプリ全体で使う stylesheet とフォントファミリ名を一元管理し、
 * ダイアログ等に適用するユーティリティ。シンプルな静的ユーティリティとして提供。
 */
public final class AppStyleUtil {

    private static String uiStylesheetUrl; // external form
    private static String fontFamily; // CSS 用にそのまま埋めるファミリ名

    private AppStyleUtil() {
        // no-op
    }

    public static void setUiStylesheetUrl(String url) {
        uiStylesheetUrl = url;
    }

    public static String getUiStylesheetUrl() {
        return uiStylesheetUrl;
    }

    public static void setFontFamily(String family) {
        fontFamily = family;
    }

    public static String getFontFamily() {
        return fontFamily;
    }

    /**
     * Alert の DialogPane にアプリの stylesheet とフォントを適用するユーティリティ。
     */
    public static void applyToAlert(Alert alert) {
        if (alert == null)
            return;
        var pane = alert.getDialogPane();
        if (uiStylesheetUrl != null && !uiStylesheetUrl.isBlank()) {
            if (!pane.getStylesheets().contains(uiStylesheetUrl)) {
                pane.getStylesheets().add(uiStylesheetUrl);
            }
        }
        if (fontFamily != null && !fontFamily.isBlank()) {
            String css = String.format("-fx-font-family: '%s','Noto Sans JP','Yu Gothic UI','Meiryo',sans-serif;",
                    fontFamily);
            pane.setStyle(css);
        }
    }
}
