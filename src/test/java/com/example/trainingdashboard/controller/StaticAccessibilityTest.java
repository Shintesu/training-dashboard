package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class StaticAccessibilityTest {

    private static final Path STATIC_DIRECTORY = Path.of("src/main/resources/static");
    private static final List<String> HTML_PAGES = List.of(
            "index.html",
            "dashboard.html",
            "comparison.html");
    private static final Pattern MAIN = Pattern.compile("<main\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern H1 = Pattern.compile("<h1\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAV_TAG = Pattern.compile("<nav\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARIA_LABEL = Pattern.compile(
            "\\baria-label\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern THEAD_BLOCK = Pattern.compile(
            "<thead\\b[^>]*>(.*?)</thead>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TH_TAG = Pattern.compile("<th\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOPE_COL = Pattern.compile(
            "\\bscope\\s*=\\s*\"col\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE_TAG = Pattern.compile("<table\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARIA_LABELLEDBY = Pattern.compile(
            "\\baria-labelledby\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INPUT_TAG = Pattern.compile("<input\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_ATTRIBUTE = Pattern.compile(
            "\\bid\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTOCOMPLETE_OFF = Pattern.compile(
            "\\bautocomplete\\s*=\\s*\"off\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_LABEL_EXPORT = Pattern.compile(
            "\\bexport\\s+function\\s+deleteButtonAriaLabel\\b");

    @Test
    void everyPageHasExactlyOneMainAndOneH1() throws IOException {
        for (String page : HTML_PAGES) {
            String html = readPage(page);

            assertThat(countMatches(html, MAIN)).as("%s main elements", page).isEqualTo(1L);
            assertThat(countMatches(html, H1)).as("%s h1 elements", page).isEqualTo(1L);
        }
    }

    @Test
    void everyPageNavigationHasAnAccessibleName() throws IOException {
        for (String page : HTML_PAGES) {
            String html = readPage(page);
            Matcher navMatcher = NAV_TAG.matcher(html);
            int navCount = 0;

            while (navMatcher.find()) {
                navCount++;
                Matcher labelMatcher = ARIA_LABEL.matcher(navMatcher.group());

                assertThat(labelMatcher.find())
                        .as("%s nav has no aria-label", page)
                        .isTrue();
                assertThat(labelMatcher.group(1).trim())
                        .as("%s nav aria-label is empty", page)
                        .isNotEmpty();
            }

            assertThat(navCount).as("%s nav elements", page).isPositive();
        }
    }

    @Test
    void everyTableHeaderCellInAHeadDeclaresAColumnScope() throws IOException {
        for (String page : HTML_PAGES) {
            String html = readPage(page);
            Matcher theadMatcher = THEAD_BLOCK.matcher(html);

            while (theadMatcher.find()) {
                Matcher thMatcher = TH_TAG.matcher(theadMatcher.group(1));

                while (thMatcher.find()) {
                    assertThat(SCOPE_COL.matcher(thMatcher.group()).find())
                            .as("%s has a th without scope=\"col\": %s", page, thMatcher.group())
                            .isTrue();
                }
            }
        }
    }

    @Test
    void everyTableIsLabelledByAnExistingElement() throws IOException {
        for (String page : HTML_PAGES) {
            String html = readPage(page);
            Matcher tableMatcher = TABLE_TAG.matcher(html);

            while (tableMatcher.find()) {
                String table = tableMatcher.group();
                Matcher labelledByMatcher = ARIA_LABELLEDBY.matcher(table);

                assertThat(labelledByMatcher.find())
                        .as("%s has a table without aria-labelledby: %s", page, table)
                        .isTrue();

                for (String id : labelledByMatcher.group(1).trim().split("\\s+")) {
                    assertThat(containsId(html, id))
                            .as("%s table references a missing id: %s", page, id)
                            .isTrue();
                }
            }
        }
    }

    @Test
    void theDurationFieldDisablesAutocomplete() throws IOException {
        String html = readPage("index.html");
        Matcher inputMatcher = INPUT_TAG.matcher(html);
        boolean durationFound = false;

        while (inputMatcher.find()) {
            String input = inputMatcher.group();
            Matcher idMatcher = ID_ATTRIBUTE.matcher(input);

            if (idMatcher.find() && idMatcher.group(1).equals("duration-minutes")) {
                durationFound = true;
                assertThat(AUTOCOMPLETE_OFF.matcher(input).find())
                        .as("duration field has no autocomplete=\"off\": %s", input)
                        .isTrue();
            }
        }

        assertThat(durationFound).as("duration field not found").isTrue();
    }

    @Test
    void theDeleteButtonAccessibleLabelIsBuiltInFormatAndUsedByRegister() throws IOException {
        String formatSource = Files.readString(STATIC_DIRECTORY.resolve("js").resolve("format.js"));
        String registerSource = Files.readString(STATIC_DIRECTORY.resolve("js").resolve("register.js"));

        assertThat(DELETE_LABEL_EXPORT.matcher(formatSource).find())
                .as("format.js does not export deleteButtonAriaLabel")
                .isTrue();
        assertThat(registerSource)
                .as("register.js does not call deleteButtonAriaLabel")
                .containsPattern("\\bdeleteButtonAriaLabel\\s*\\(");
    }

    private static String readPage(String page) throws IOException {
        return Files.readString(STATIC_DIRECTORY.resolve(page));
    }

    private static long countMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        long count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private static boolean containsId(String html, String id) {
        Pattern idPattern = Pattern.compile(
                "\\bid\\s*=\\s*\"" + Pattern.quote(id) + "\"",
                Pattern.CASE_INSENSITIVE);
        return idPattern.matcher(html).find();
    }
}
