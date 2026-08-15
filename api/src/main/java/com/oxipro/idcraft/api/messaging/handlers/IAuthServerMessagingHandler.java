package com.oxipro.idcraft.api.messaging.handlers;

import com.oxipro.idcraft.api.auth.AuthVisitKind;

import java.util.UUID;

public interface IAuthServerMessagingHandler {
    void handle(UUID player, AuthVisitKind kind);
}
