package com.pulseforge;

import com.pulseforge.model.RunRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PostgresPersistenceTest {
    @Autowired
    private TestRestTemplate rest;

    @Test
    void persistsQueuedRunsAndRequiresConfirmation() {
        RunRequest request = new RunRequest();
        request.setName("checkout");
        request.setUrl("http://127.0.0.1:8081/api/checkout");
        request.setMethod("GET");
        request.setUsers(2);
        request.setDurationSeconds(1);
        request.setPattern("baseline");

        ResponseEntity<Map> created = rest.postForEntity("/api/runs", request, Map.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        String id = String.valueOf(created.getBody().get("id"));

        ResponseEntity<Map[]> listed = rest.getForEntity("/api/runs", Map[].class);
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertTrue(listed.getBody().length >= 1);

        ResponseEntity<Map> refused = rest.postForEntity("/api/runs/" + id + "/start", null, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, refused.getStatusCode());

        rest.postForEntity("/api/runs/" + id + "/start?confirm=true", null, Map.class);
        ResponseEntity<Map> stored = rest.getForEntity("/api/runs/" + id, Map.class);
        assertEquals(HttpStatus.OK, stored.getStatusCode());
        assertEquals(id, stored.getBody().get("id"));
    }

    @Test
    void rejectsOversizedRuns() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
            {"name":"too-big","url":"http://127.0.0.1:8081/api/checkout","method":"GET","users":900,"durationSeconds":10,"pattern":"baseline"}
            """;
        ResponseEntity<String> response = rest.postForEntity("/api/runs", new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
