package com.example.trainingdashboard.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.trainingdashboard.repository.WorkoutRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final String CONNECTION_STRING =
            "jdbc:postgresql://leak.invalid:5432/secret?password=super-secret-probe";
    private static final String PASSWORD = "super-secret-probe";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkoutRepository workoutRepository;

    @Test
    void returnsGenericInternalErrorWithoutLeakingDetails(CapturedOutput output) throws Exception {
        when(workoutRepository.findAllByOrderByDateDescIdDesc())
                .thenThrow(new DataAccessResourceFailureException("Could not open " + CONNECTION_STRING));

        MvcResult result = mockMvc.perform(get("/api/workouts")).andReturn();
        String responseBody = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat((String) JsonPath.read(responseBody, "$.message"))
                .isEqualTo("Ocurrió un error inesperado. Inténtalo de nuevo más tarde");
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors")).isEmpty();
        assertThat(responseBody).doesNotContain("jdbc", "password", CONNECTION_STRING, PASSWORD);
        assertThat(output.getAll()).doesNotContain(CONNECTION_STRING, PASSWORD);
        assertThat(output.getAll()).contains("DataAccessResourceFailureException");
    }

    @Test
    void keepsDefaultNotFoundForUnknownRoute() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/no-existe")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void keepsDefaultMethodNotAllowed() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/workouts/1")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(405);
    }

    @Test
    void keepsDefaultUnsupportedMediaType() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workouts")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(415);
    }
}
