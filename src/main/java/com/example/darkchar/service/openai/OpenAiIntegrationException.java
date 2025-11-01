package com.example.darkchar.service.openai;

/**
 * OpenAI APIとの通信時に発生した問題を示す例外。
 */
public class OpenAiIntegrationException extends RuntimeException {

    /**
     * メッセージのみで例外を生成します。
     *
     * @param message エラーメッセージ
     */
    public OpenAiIntegrationException(String message) {
        super(message);
    }

    /**
     * メッセージと原因例外で生成します。
     *
     * @param message エラーメッセージ
     * @param cause   原因となった例外
     */
    public OpenAiIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
