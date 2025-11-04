package com.example.darkchar.ui;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Pattern;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

/**
 * ユーザー入力欄で日本語 IME の入力を扱いやすくするためのユーティリティです。
 * <p>
 * JavaFX の {@link TextFormatter} を用いて入力テキストを NFKC 互換の形式に正規化し、
 * 制御文字を取り除くことで、フォーム上のテキストフィールドやテキストエリアに
 * 日本語をスムーズに入力できるようにします。
 */
public final class JapaneseTextInputSupport {

    private static final Pattern DISALLOWED_CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\t\\r\\n]]");

    private JapaneseTextInputSupport() {
        // utility class
    }

    /**
     * 指定したテキスト入力コントロールに日本語入力サポートを適用します。
     *
     * @param control 対象のテキスト入力コントロール
     */
    public static void enable(TextInputControl control) {
        if (control == null) {
            return;
        }

        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            if (change == null || !change.isContentChange()) {
                return change;
            }

            String text = change.getText();
            if (text == null || text.isEmpty()) {
                return change;
            }

            String normalized = Normalizer.normalize(text, Form.NFKC);
            String sanitized = DISALLOWED_CONTROL.matcher(normalized).replaceAll("");
            change.setText(sanitized);
            return change;
        });

        control.setTextFormatter(formatter);
    }
}

