package com.example.darkchar.service.openai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpenAiRequest(
        String model,
        List<OpenAiMessage> input,
        Double temperature,
        @JsonProperty("max_output_tokens") Integer maxOutputTokens) {
}
