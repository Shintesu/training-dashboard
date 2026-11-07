package com.example.trainingdashboard.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictWorkoutJsonCustomizer() {
        // SPECS §10 rejects non-integer durations, numeric enums and non-ISO dates;
        // Jackson 3 defaults coerce or truncate those values silently.
        return builder -> builder
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Array, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Object, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .withCoercionConfig(LogicalType.Enum, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Array, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Object, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .addModule(strictLocalDateModule());
    }

    private SimpleModule strictLocalDateModule() {
        SimpleModule module = new SimpleModule("strict-local-date");
        module.addDeserializer(LocalDate.class, new StrictLocalDateDeserializer());
        return module;
    }

    private static final class StrictLocalDateDeserializer extends StdDeserializer<LocalDate> {

        private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        private static final String MESSAGE = "Expected an ISO date string (YYYY-MM-DD)";

        private StrictLocalDateDeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return (LocalDate) context.reportInputMismatch(LocalDate.class, MESSAGE);
            }
            String value = parser.getString();
            if (value.isEmpty()) {
                return null;
            }
            if (!ISO_DATE.matcher(value).matches()) {
                return (LocalDate) context.handleWeirdStringValue(LocalDate.class, value, MESSAGE);
            }
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException exception) {
                return (LocalDate) context.handleWeirdStringValue(LocalDate.class, value, MESSAGE);
            }
        }
    }
}
