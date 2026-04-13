package com.brewmaster.calendar;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: implement in Calendar feature milestone
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Calendar")
public class BrewEventController {

    private final BrewEventService brewEventService;

    public BrewEventController(BrewEventService brewEventService) {
        this.brewEventService = brewEventService;
    }
}
