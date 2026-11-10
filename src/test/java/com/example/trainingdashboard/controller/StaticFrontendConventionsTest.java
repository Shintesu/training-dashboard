package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class StaticFrontendConventionsTest {

    private static final Path STATIC_DIRECTORY = Path.of("src/main/resources/static");
    private static final List<String> HTML_PAGES = List.of(
            "index.html",
            "dashboard.html",
            "comparison.html");
    private static final Pattern INPUT_OR_SELECT = Pattern.compile(
            "<(?:input|select)\\b[^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_ATTRIBUTE = Pattern.compile("\\bid=\"([^\"]+)\"");
    private static final Pattern SCRIPT_TAG = Pattern.compile("<script\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_HANDLER = Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_STYLE = Pattern.compile("\\sstyle\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_BLOCK = Pattern.compile("<style\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMPORT_STATEMENT = Pattern.compile(
            "import\\s+\\{([^}]*)\\}\\s+from\\s+\"(\\./[^\"]+)\";",
            Pattern.DOTALL);

    @Test
    void doesNotUseForbiddenDomOrDynamicCodeApis() throws IOException {
        List<String> forbiddenFragments = List.of(
                "innerHTML",
                "outerHTML",
                "insertAdjacentHTML",
                "document.write",
                "eval(");

        for (Path file : allFiles()) {
            String content = Files.readString(file);
            for (String fragment : forbiddenFragments) {
                assertThat(content)
                        .as("%s contains %s", file, fragment)
                        .doesNotContain(fragment);
            }
        }
    }

    @Test
    void doesNotUseVarInJavaScript() throws IOException {
        Pattern varDeclaration = Pattern.compile("\\bvar\\b");

        for (Path file : filesWithExtension("js")) {
            String content = Files.readString(file);

            assertThat(varDeclaration.matcher(content).find())
                    .as("%s uses var", file)
                    .isFalse();
        }
    }

    @Test
    void onlyApiModuleCallsFetch() throws IOException {
        for (Path file : filesWithExtension("js")) {
            String content = Files.readString(file);
            boolean callsFetch = content.contains("fetch(");

            if (file.getFileName().toString().equals("api.js")) {
                assertThat(callsFetch).as("api.js must call fetch").isTrue();
            } else {
                assertThat(callsFetch).as("%s must not call fetch", file).isFalse();
            }
        }
    }

    @Test
    void htmlDoesNotUseInlineHandlersStylesOrStyleBlocks() throws IOException {
        for (String page : HTML_PAGES) {
            String content = Files.readString(STATIC_DIRECTORY.resolve(page));

            assertThat(INLINE_HANDLER.matcher(content).find())
                    .as("%s uses an inline handler", page)
                    .isFalse();
            assertThat(INLINE_STYLE.matcher(content).find())
                    .as("%s uses an inline style", page)
                    .isFalse();
            assertThat(STYLE_BLOCK.matcher(content).find())
                    .as("%s has a style block", page)
                    .isFalse();
        }
    }

    @Test
    void cssDoesNotUseImportant() throws IOException {
        for (Path file : filesWithExtension("css")) {
            String content = Files.readString(file);

            assertThat(content).as("%s uses !important", file).doesNotContain("!important");
        }
    }

    @Test
    void containsNoExternalUrls() throws IOException {
        for (Path file : allFiles()) {
            String content = Files.readString(file);

            assertThat(content)
                    .as("%s contains an external URL", file)
                    .doesNotContain("http://", "https://");
        }
    }

    @Test
    void everyPageLoadsExactlyOneModuleScript() throws IOException {
        for (String page : HTML_PAGES) {
            String content = Files.readString(STATIC_DIRECTORY.resolve(page));
            Matcher matcher = SCRIPT_TAG.matcher(content);
            List<String> scriptTags = new ArrayList<>();
            while (matcher.find()) {
                scriptTags.add(matcher.group());
            }

            assertThat(scriptTags).as("%s script tags", page).hasSize(1);
            assertThat(scriptTags.get(0)).as("%s module script", page).contains("type=\"module\"");
        }
    }

    @Test
    void everyInputAndSelectHasALabel() throws IOException {
        for (String page : HTML_PAGES) {
            String content = Files.readString(STATIC_DIRECTORY.resolve(page));
            Matcher matcher = INPUT_OR_SELECT.matcher(content);
            while (matcher.find()) {
                String tag = matcher.group();
                Matcher idMatcher = ID_ATTRIBUTE.matcher(tag);

                assertThat(idMatcher.find())
                        .as("%s has an input or select without id: %s", page, tag)
                        .isTrue();
                assertThat(hasLabelFor(content, idMatcher.group(1)))
                        .as("%s lacks a label for %s", page, idMatcher.group(1))
                        .isTrue();
            }
        }
    }

    @Test
    void everyNamedImportFromALocalModuleResolves() throws IOException {
        Pattern exportPattern = Pattern.compile(
                "\\bexport\\s+(?:const|let|var|function|class|async\\s+function)\\s+");
        int importsFound = 0;

        for (Path file : filesWithExtension("js")) {
            String content = Files.readString(file);
            Matcher matcher = IMPORT_STATEMENT.matcher(content);

            while (matcher.find()) {
                importsFound++;
                String importedNames = matcher.group(1);
                String modulePath = matcher.group(2);
                Path moduleFile = file.getParent().resolve(modulePath).normalize();

                assertThat(Files.isRegularFile(moduleFile))
                        .as("%s imports a missing module: %s", file, modulePath)
                        .isTrue();

                String moduleSource = Files.readString(moduleFile);
                for (String rawName : importedNames.split(",")) {
                    String name = rawName.trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    Pattern namedExport = Pattern.compile(
                            exportPattern.pattern() + Pattern.quote(name) + "\\b");
                    assertThat(namedExport.matcher(moduleSource).find())
                            .as("%s imports %s from %s, but it is not exported", file, name, modulePath)
                            .isTrue();
                }
            }
        }

        assertThat(importsFound).as("local named imports found").isPositive();
    }

    private static List<Path> allFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(STATIC_DIRECTORY)) {
            return paths
                    .filter(path -> Files.isRegularFile(path))
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> filesWithExtension(String extension) throws IOException {
        return allFiles().stream()
                .filter(path -> path.getFileName().toString().endsWith("." + extension))
                .toList();
    }

    private static boolean hasLabelFor(String html, String id) {
        Pattern label = Pattern.compile(
                "<label\\b[^>]*\\bfor=\"" + Pattern.quote(id) + "\"",
                Pattern.CASE_INSENSITIVE);
        return label.matcher(html).find();
    }
}
