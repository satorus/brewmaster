package com.brewmaster.calendar;

import com.brewmaster.calendar.dto.*;
import com.brewmaster.user.User;
import com.brewmaster.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class BrewEventService {

    private final BrewEventRepository eventRepository;
    private final UserRepository userRepository;

    public BrewEventService(BrewEventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public List<BrewEventResponse> getEventsByMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return eventRepository.findByMonthWithParticipants(start, end)
                .stream().map(this::toResponse).toList();
    }

    public BrewEventResponse getEvent(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BrewEventResponse createEvent(CreateEventRequest req, User creator) {
        BrewEvent event = new BrewEvent(
                req.title(), req.description(), req.brewDate(),
                req.startTime(), req.location(), req.recipeId(), creator.getId());

        BrewEvent saved = eventRepository.save(event);

        saved.getParticipants().add(new BrewEventParticipant(saved, creator, "ACCEPTED"));

        if (req.invitedUserIds() != null) {
            for (UUID userId : req.invitedUserIds()) {
                if (userId.equals(creator.getId())) continue;
                User invited = userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Invited user not found: " + userId));
                saved.getParticipants().add(new BrewEventParticipant(saved, invited, "PENDING"));
            }
        }

        return toResponse(eventRepository.save(saved));
    }

    @Transactional
    public BrewEventResponse updateEvent(UUID id, UpdateEventRequest req, User user) {
        BrewEvent event = findOrThrow(id);
        requireCreator(event, user);
        event.update(req.title(), req.description(), req.brewDate(),
                req.startTime(), req.location(), req.recipeId());
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(UUID id, User user) {
        BrewEvent event = findOrThrow(id);
        requireCreator(event, user);
        eventRepository.delete(event);
    }

    @Transactional
    public BrewEventResponse rsvp(UUID eventId, RsvpRequest req, User user) {
        BrewEvent event = findOrThrow(eventId);

        event.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .ifPresentOrElse(
                        p -> p.setRsvp(req.status()),
                        () -> event.getParticipants().add(new BrewEventParticipant(event, user, req.status()))
                );

        return toResponse(eventRepository.save(event));
    }

    private BrewEvent findOrThrow(UUID id) {
        return eventRepository.findByIdWithParticipants(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void requireCreator(BrewEvent event, User user) {
        if (!event.getCreatedBy().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event creator can perform this action");
        }
    }

    private BrewEventResponse toResponse(BrewEvent event) {
        List<ParticipantDto> participants = event.getParticipants().stream()
                .map(p -> new ParticipantDto(
                        p.getUser().getId(),
                        p.getUser().getUsername(),
                        p.getUser().getDisplayName(),
                        p.getRsvp()))
                .toList();

        return new BrewEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getBrewDate(),
                event.getStartTime(),
                event.getLocation(),
                event.getRecipeId(),
                event.getCreatedBy(),
                event.getCreatedAt().toString(),
                participants);
    }
}
