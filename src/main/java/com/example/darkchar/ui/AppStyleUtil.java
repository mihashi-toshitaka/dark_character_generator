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

    /**
     * UI 用スタイルシートの URL を保存します。
     *
     * @param url スタイルシート URL
     */
    public static void setUiStylesheetUrl(String url) {
        uiStylesheetUrl = url;
    }

    /**
     * 保存されたスタイルシート URL を取得します。
     *
     * @return スタイルシート URL
     */
    public static String getUiStylesheetUrl() {
        return uiStylesheetUrl;
    }

    /**
     * 共通で使用するフォントファミリを設定します。
     *
     * @param family フォントファミリ名
     */
    public static void setFontFamily(String family) {
        fontFamily = family;
    }

    /**
     * 保存されたフォントファミリを取得します。
     *
     * @return フォントファミリ名
     */
    public static String getFontFamily() {
        return fontFamily;
    }

    /**
     * Alert の DialogPane にアプリの stylesheet とフォントを適用します。
     *
     * @param alert 対象のアラート
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
