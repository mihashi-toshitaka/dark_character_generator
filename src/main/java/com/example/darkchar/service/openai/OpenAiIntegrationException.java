package com.example.darkchar.service.openai;

/**
 * OpenAI APIとの通信時に発生した問題を示す例外。
 */
public class OpenAiIntegrationException extends RuntimeException {

    public OpenAiIntegrationException(String message) {
        super(message);
    }

    public OpenAiIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
