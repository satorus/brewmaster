package com.brewmaster.calendar;

import com.brewmaster.auth.dto.LoginRequest;
import com.brewmaster.auth.dto.RegisterRequest;
import com.brewmaster.calendar.dto.CreateEventRequest;
import com.brewmaster.calendar.dto.RsvpRequest;
import com.brewmaster.calendar.dto.UpdateEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrewEventControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String token2;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("brewer1", "brewer1@test.com", "password123");
        token2 = registerAndLogin("brewer2", "brewer2@test.com", "password123");
    }

    @Test
    void createEvent_shouldReturn201WithParticipant() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "Test Brew Day", "A great day of brewing",
                LocalDate.now().plusDays(7), null, "My Garage", null, null);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Brew Day"))
                .andExpect(jsonPath("$.participants[0].rsvp").value("ACCEPTED"));
    }

    @Test
    void createEvent_shouldReturn401WithoutToken() throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                "No Auth Event", null, LocalDate.now().plusDays(1), null, null, null, null);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEvent_shouldReturn400ForMissingTitle() throws Exception {
        String body = "{\"brewDate\": \"" + LocalDate.now().plusDays(1) + "\"}";

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_shouldReturnEventsForMonth() throws Exception {
        LocalDate date = LocalDate.now().withDayOfMonth(15);
        CreateEventRequest req = new CreateEventRequest(
                "Monthly Event", null, date, null, null, null, null);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        String month = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        mockMvc.perform(get("/api/v1/events?month=" + month)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Monthly Event"));
    }

    @Test
    void getEventById_shouldReturnEvent() throws Exception {
        String eventId = createEventAndGetId("Single Event Fetch", token);

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Single Event Fetch"));
    }

    @Test
    void getEventById_shouldReturn404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/v1/events/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEvent_shouldReturn200ForCreator() throws Exception {
        String eventId = createEventAndGetId("Original Title", token);

        UpdateEventRequest update = new UpdateEventRequest(
                "Updated Title", "New description",
                LocalDate.now().plusDays(14), null, "New Location", null);

        mockMvc.perform(put("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.location").value("New Location"));
    }

    @Test
    void updateEvent_shouldReturn403ForNonCreator() throws Exception {
        String eventId = createEventAndGetId("Someone Else's Event", token);

        UpdateEventRequest update = new UpdateEventRequest(
                "Hijacked Title", null, LocalDate.now().plusDays(1), null, null, null);

        mockMvc.perform(put("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvent_shouldReturn204ForCreator() throws Exception {
        String eventId = createEventAndGetId("To Be Deleted", token);

        mockMvc.perform(delete("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEvent_shouldReturn403ForNonCreator() throws Exception {
        String eventId = createEventAndGetId("Protected Event", token);

        mockMvc.perform(delete("/api/v1/events/" + eventId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void rsvp_shouldUpdateStatus() throws Exception {
        String eventId = createEventAndGetId("RSVP Event", token);
        RsvpRequest req = new RsvpRequest("ACCEPTED");

        mockMvc.perform(post("/api/v1/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[?(@.rsvp == 'ACCEPTED')]").isNotEmpty());
    }

    @Test
    void rsvp_shouldReturn400ForInvalidStatus() throws Exception {
        String eventId = createEventAndGetId("RSVP Validation Event", token);

        mockMvc.perform(post("/api/v1/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"MAYBE\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

    private String registerAndLogin(String username, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest(username, email, password, null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String createEventAndGetId(String title, String authToken) throws Exception {
        CreateEventRequest req = new CreateEventRequest(
                title, null, LocalDate.now().plusDays(7), null, null, null, null);

        MvcResult result = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}
