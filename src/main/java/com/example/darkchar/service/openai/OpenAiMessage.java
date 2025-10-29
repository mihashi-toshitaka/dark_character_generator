package com.example.darkchar.service.openai;

import java.util.List;

record OpenAiMessage(String role, List<OpenAiContent> content) {
}
