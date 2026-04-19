package com.brewmaster.brew;

import com.brewmaster.brew.dto.*;
import com.brewmaster.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Brew Sessions")
public class BrewSessionController {

    private final BrewSessionService brewSessionService;

    public BrewSessionController(BrewSessionService brewSessionService) {
        this.brewSessionService = brewSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrewSessionResponse startSession(@Valid @RequestBody StartSessionRequest req,
                                            @AuthenticationPrincipal User user) {
        return brewSessionService.startSession(req, user);
    }

    @GetMapping("/{id}")
    public BrewSessionResponse getSession(@PathVariable UUID id) {
        return brewSessionService.getSession(id);
    }

    @PutMapping("/{id}/step")
    public BrewSessionResponse advanceStep(@PathVariable UUID id,
                                           @Valid @RequestBody AdvanceStepRequest req,
                                           @AuthenticationPrincipal User user) {
        return brewSessionService.advanceStep(id, req, user);
    }

    @PutMapping("/{id}/complete")
    public BrewSessionResponse completeSession(@PathVariable UUID id,
                                               @RequestBody(required = false) CompleteSessionRequest req,
                                               @AuthenticationPrincipal User user) {
        return brewSessionService.completeSession(id, req != null ? req : new CompleteSessionRequest(null), user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abandonSession(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        brewSessionService.abandonSession(id, user);
    }
}
