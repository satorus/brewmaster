package com.brewmaster.brew;

import org.springframework.stereotype.Service;

// TODO: implement in Brew Mode feature milestone
@Service
public class BrewSessionService {

    private final BrewSessionRepository brewSessionRepository;

    public BrewSessionService(BrewSessionRepository brewSessionRepository) {
        this.brewSessionRepository = brewSessionRepository;
    }
}
