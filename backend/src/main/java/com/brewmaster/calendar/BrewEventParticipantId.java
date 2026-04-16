package com.brewmaster.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BrewEventParticipantId implements Serializable {

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "user_id")
    private UUID userId;

    protected BrewEventParticipantId() {}

    public BrewEventParticipantId(UUID eventId, UUID userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    public UUID getEventId() { return eventId; }
    public UUID getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrewEventParticipantId that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(eventId, userId); }
}
