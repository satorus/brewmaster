package com.brewmaster.calendar;

import com.brewmaster.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "brew_event_participants")
public class BrewEventParticipant {

    @EmbeddedId
    private BrewEventParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id")
    private BrewEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 20)
    private String rsvp = "PENDING";

    protected BrewEventParticipant() {}

    public BrewEventParticipant(BrewEvent event, User user, String rsvp) {
        this.event = event;
        this.user = user;
        this.rsvp = rsvp;
        this.id = new BrewEventParticipantId(event.getId(), user.getId());
    }

    public BrewEventParticipantId getId() { return id; }
    public BrewEvent getEvent() { return event; }
    public User getUser() { return user; }
    public String getRsvp() { return rsvp; }
    public void setRsvp(String rsvp) { this.rsvp = rsvp; }
}
