package com.brewmaster.calendar;

import org.springframework.stereotype.Service;

// TODO: implement in Calendar feature milestone
@Service
public class BrewEventService {

    private final BrewEventRepository brewEventRepository;

    public BrewEventService(BrewEventRepository brewEventRepository) {
        this.brewEventRepository = brewEventRepository;
    }
}
