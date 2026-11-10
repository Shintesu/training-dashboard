package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticPagesTest {

    private static final Path STATIC_DIRECTORY = Path.of("src/main/resources/static");
    private static final List<String> HTML_PAGES = List.of(
            "index.html",
            "dashboard.html",
            "comparison.html");
    private static final Pattern LOCAL_LINK = Pattern.compile("(?:src|href)=\"(/[^\"]*)\"");
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesIndexAtRootAndAsFile() throws Exception {
        assertHtml("/");
        assertHtml("/index.html");
    }

    @Test
    void servesDashboardAndComparisonPagesWithHtml() throws Exception {
        assertHtml("/dashboard.html");
        assertHtml("/comparison.html");
    }

    @Test
    void servesStylesheetWithCssContentType() throws Exception {
        MvcResult result = mockMvc.perform(get("/styles.css")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).contains("text/css");
    }

    @Test
    void servesEveryScriptModuleWithJavascriptContentType() throws Exception {
        try (Stream<Path> files = Files.list(STATIC_DIRECTORY.resolve("js"))) {
            List<String> scriptPaths = files
                    .filter(path -> path.getFileName().toString().endsWith(".js"))
                    .map(path -> "/js/" + path.getFileName())
                    .sorted()
                    .toList();

            assertThat(scriptPaths).isNotEmpty();

            for (String scriptPath : scriptPaths) {
                MvcResult result = mockMvc.perform(get(scriptPath)).andReturn();

                assertThat(result.getResponse().getStatus())
                        .as("GET %s", scriptPath)
                        .isEqualTo(200);
                assertThat(result.getResponse().getContentType())
                        .as("GET %s", scriptPath)
                        .containsIgnoringCase("javascript");
            }
        }
    }

    @Test
    void servesEveryLocalLinkFoundInThePages() throws Exception {
        Set<String> localLinks = new TreeSet<>();
        for (String page : HTML_PAGES) {
            String html = Files.readString(STATIC_DIRECTORY.resolve(page));
            Matcher matcher = LOCAL_LINK.matcher(html);
            while (matcher.find()) {
                localLinks.add(matcher.group(1));
            }
        }

        assertThat(localLinks).isNotEmpty();

        for (String link : localLinks) {
            MvcResult result = mockMvc.perform(get(link)).andReturn();

            assertThat(result.getResponse().getStatus())
                    .as("GET %s", link)
                    .isEqualTo(200);
        }
    }

    @Test
    void everyPageHasLanguageTitleMenuAndOneCurrentLink() throws Exception {
        for (String page : HTML_PAGES) {
            String html = Files.readString(STATIC_DIRECTORY.resolve(page));

            assertThat(html).as(page).contains("lang=\"es\"");
            assertThat(TITLE.matcher(html).results().count()).as(page).isEqualTo(1L);
            assertThat(html).as(page)
                    .contains("href=\"/\"")
                    .contains("href=\"/dashboard.html\"")
                    .contains("href=\"/comparison.html\"")
                    .contains(">Registro</a>")
                    .contains(">Dashboard</a>")
                    .contains(">Comparativa</a>");
            assertThat(countOccurrences(html, "aria-current=\"page\"")).as(page).isEqualTo(1);
        }
    }

    @Test
    void keepsWorkoutApiReturningJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/workouts")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).contains("application/json");
    }

    private void assertHtml(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("GET %s", path)
                .isEqualTo(200);

        String forwardedUrl = result.getResponse().getForwardedUrl();
        if (forwardedUrl != null) {
            // MockMvc records the welcome-page forward instead of rendering the static file.
            assertThat(forwardedUrl).as("GET %s", path).isEqualTo("index.html");
            return;
        }

        assertThat(result.getResponse().getContentType())
                .as("GET %s", path)
                .contains("text/html");
    }

    private static long countOccurrences(String text, String needle) {
        return text.split(Pattern.quote(needle), -1).length - 1L;
    }
}
