package io.debezium.connector.postgresql.connection.wal2json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DateParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateParser.class);

    private static final List<DateTimeFormatter> timestampDateTimeFormatters = initializeTimestampDateTimeFormatters();

    private static final List<DateTimeFormatter> dayDateTimeFormatters = initializeDayDateTimeFormatters();

    private static List<DateTimeFormatter> initializeTimestampDateTimeFormatters() {
        // Postgresql years can be as large as 6 digits. See https://www.postgresql.org/docs/9.6/datatype-datetime.html
        // Try parsing it with 4 digits first since that will be the common case. Then try up to 6 digits.
        List<Integer> yearDigitsOrder = Arrays.asList(4, 1, 2, 3, 5, 6);

        return yearDigitsOrder
                .stream()
                .map(DateParser::createTimestampDateTimeFormatter)
                .collect(Collectors.toList());
    }

    private static List<DateTimeFormatter> initializeDayDateTimeFormatters() {
        List<Integer> yearDigitsOrder = Arrays.asList(4, 1, 2, 3, 5, 6, 7);

        return yearDigitsOrder
                .stream()
                .map(DateParser::createDayDateTimeFormatter)
                .collect(Collectors.toList());
    }

    private static DateTimeFormatter createDayDateTimeFormatter(int yearDigits) {
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < yearDigits; i++) {
            pattern.append("y");
        }

        pattern.append("-MM-dd[ GG]");

        return DateTimeFormatter.ofPattern(pattern.toString());
    }

    private static DateTimeFormatter createTimestampDateTimeFormatter(int yearDigits) {
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < yearDigits; i++) {
            pattern.append("y");
        }

        pattern.append("-MM-dd HH:mm:ss");

        return new DateTimeFormatterBuilder()
                .appendPattern(pattern.toString())
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .optionalStart()
                .appendLiteral(" ")
                .appendText(ChronoField.ERA, TextStyle.SHORT)
                .optionalEnd()
                .toFormatter();
    }

    private static LocalDateTime tryParseLocalDateTime(String text, DateTimeFormatter dateTimeFormatter) {
        try {
            return LocalDateTime.parse(text, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate tryParseLocalDate(String text, DateTimeFormatter dateTimeFormatter) {
        try {
            return LocalDate.parse(text, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDateTime parsePostgresTimestampWithoutTimeZone(String text) {
        LocalDateTime localDateTime;

        for (DateTimeFormatter dateTimeFormatter : timestampDateTimeFormatters) {
            localDateTime = tryParseLocalDateTime(text, dateTimeFormatter);

            if (localDateTime != null) {
                return localDateTime;
            }
        }

        throw new RuntimeException("Could not successfully parse: " + text);
    }

    public static LocalDate parsePostgresDate(String text) {
        LocalDate localDate;

        for (DateTimeFormatter dateTimeFormatter : dayDateTimeFormatters) {
            localDate = tryParseLocalDate(text, dateTimeFormatter);

            if (localDate != null) {
                return localDate;
            }
        }

        throw new RuntimeException("Could not successfully parse: " + text);
    }
}
