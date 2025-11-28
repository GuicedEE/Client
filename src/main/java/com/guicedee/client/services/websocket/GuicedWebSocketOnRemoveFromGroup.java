package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;

import java.util.concurrent.CompletableFuture;

public interface GuicedWebSocketOnRemoveFromGroup<J extends GuicedWebSocketOnRemoveFromGroup<J>>  extends IDefaultService<J> {
    /**
     * Returns true if the action was already taken
     * @param groupName
     * @return
     */
    CompletableFuture<Boolean> onRemoveFromGroup(String groupName);
}
