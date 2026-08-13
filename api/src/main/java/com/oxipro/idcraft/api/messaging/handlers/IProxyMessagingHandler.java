package com.oxipro.idcraft.api.messaging.handlers;

import java.util.UUID;

public interface IProxyMessagingHandler {
   void handle(String fromServer, UUID player);
}
