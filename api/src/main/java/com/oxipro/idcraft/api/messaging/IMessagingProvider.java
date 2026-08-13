package com.oxipro.idcraft.api.messaging;

import com.oxipro.idcraft.api.messaging.handlers.IAuthServerMessagingHandler;
import com.oxipro.idcraft.api.messaging.handlers.IProxyMessagingHandler;

import java.util.UUID;

public interface IMessagingProvider {

    boolean connectForProxy(IProxyMessagingHandler pmh);

    boolean connectForAuthServer(IAuthServerMessagingHandler asmh);

    boolean authenticated(UUID uuid, String authServerName);

    boolean allowConnection(UUID uuid);

    default void disconnect() {}

}
