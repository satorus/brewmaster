package com.brewmaster.user;

import java.util.UUID;

public record UserSearchDto(UUID id, String username, String displayName) {}
