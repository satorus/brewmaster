package com.brewmaster.calendar;

import com.brewmaster.calendar.dto.*;
import com.brewmaster.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Calendar")
public class BrewEventController {

    private final BrewEventService service;

    public BrewEventController(BrewEventService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List events for a given month")
    public ResponseEntity<List<BrewEventResponse>> list(
            @RequestParam(required = false) String month) {
        YearMonth ym = (month != null && !month.isBlank()) ? YearMonth.parse(month) : YearMonth.now();
        return ResponseEntity.ok(service.getEventsByMonth(ym));
    }

    @PostMapping
    @Operation(summary = "Create a new brew event")
    public ResponseEntity<BrewEventResponse> create(
            @Valid @RequestBody CreateEventRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEvent(req, user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single brew event")
    public ResponseEntity<BrewEventResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getEvent(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a brew event (creator only)")
    public ResponseEntity<BrewEventResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.updateEvent(id, req, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a brew event (creator only)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        service.deleteEvent(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rsvp")
    @Operation(summary = "RSVP to a brew event")
    public ResponseEntity<BrewEventResponse> rsvp(
            @PathVariable UUID id,
            @Valid @RequestBody RsvpRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.rsvp(id, req, user));
    }
}
