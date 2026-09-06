package com.pulseforge.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HeadersDeserializer extends JsonDeserializer<Map<String, String>> {
    @Override
    public Map<String, String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return new LinkedHashMap<>();
        }
        if (token == JsonToken.START_OBJECT) {
            Map<String, String> headers = parser.readValueAs(new TypeReference<LinkedHashMap<String, String>>() {});
            return headers == null ? new LinkedHashMap<>() : headers;
        }
        if (token == JsonToken.VALUE_STRING) {
            Map<String, String> headers = new LinkedHashMap<>();
            String raw = parser.getValueAsString();
            if (raw == null || raw.isBlank()) {
                return headers;
            }
            for (String part : raw.split("\\r?\\n|;")) {
                int index = part.indexOf(':');
                if (index > 0) {
                    headers.put(part.substring(0, index).trim(), part.substring(index + 1).trim());
                }
            }
            return headers;
        }
        parser.skipChildren();
        return new LinkedHashMap<>();
    }
}
