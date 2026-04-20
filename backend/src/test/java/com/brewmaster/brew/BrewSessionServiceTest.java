package com.brewmaster.brew;

import com.brewmaster.brew.dto.AdvanceStepRequest;
import com.brewmaster.recipe.RecipeService;
import com.brewmaster.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrewSessionServiceTest {

    @Mock BrewSessionRepository sessionRepository;
    @Mock BrewSessionStepLogRepository stepLogRepository;
    @Mock RecipeService recipeService;
    @InjectMocks BrewSessionService brewSessionService;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherId = UUID.randomUUID();

    private BrewSession inProgressSession;
    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        // Wire ObjectMapper manually since @InjectMocks won't get the Spring-managed one
        brewSessionService = new BrewSessionService(
                sessionRepository, stepLogRepository, recipeService, new ObjectMapper());

        inProgressSession = new BrewSession(
                UUID.randomUUID(), null, new BigDecimal("20"), ownerId,
                "[]", "[]", BigDecimal.TEN, new BigDecimal("3.0"),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null);
        // Override generated ID via reflection so we can reference it in queries
        setId(inProgressSession, sessionId);

        owner = mockUser(ownerId);
        otherUser = mockUser(otherId);
    }

    @Test
    void abandonSession_setsStatusAbandonedAndCallsSave() {
        when(sessionRepository.findByIdWithStepLogs(sessionId)).thenReturn(Optional.of(inProgressSession));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        brewSessionService.abandonSession(sessionId, owner);

        ArgumentCaptor<BrewSession> captor = ArgumentCaptor.forClass(BrewSession.class);
        verify(sessionRepository).save(captor.capture());
        verify(sessionRepository, never()).delete(any());
        verify(sessionRepository, never()).deleteById(any());

        assertThat(captor.getValue().getStatus()).isEqualTo("ABANDONED");
    }

    @Test
    void abandonSession_returns403WhenNotOwner() {
        when(sessionRepository.findByIdWithStepLogs(sessionId)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> brewSessionService.abandonSession(sessionId, otherUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(403);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void advanceStep_isIdempotent() {
        when(sessionRepository.findByIdWithStepLogs(sessionId)).thenReturn(Optional.of(inProgressSession));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stepLogRepository.existsBySessionIdAndStepNumber(sessionId, 0)).thenReturn(true);
        when(stepLogRepository.findBySessionIdOrderByStepNumberAsc(sessionId)).thenReturn(new ArrayList<>());

        brewSessionService.advanceStep(sessionId, new AdvanceStepRequest(0, null, null), owner);

        verify(stepLogRepository, never()).save(any());
    }

    // --- helpers ---

    private User mockUser(UUID id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    private void setId(BrewSession session, UUID id) {
        try {
            var f = BrewSession.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(session, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
