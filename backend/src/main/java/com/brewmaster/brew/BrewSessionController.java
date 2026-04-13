package com.brewmaster.brew;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: implement in Brew Mode feature milestone
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Brew Sessions")
public class BrewSessionController {

    private final BrewSessionService brewSessionService;

    public BrewSessionController(BrewSessionService brewSessionService) {
        this.brewSessionService = brewSessionService;
    }
}
