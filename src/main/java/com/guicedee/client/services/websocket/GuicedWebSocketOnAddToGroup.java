package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;

import java.util.concurrent.CompletableFuture;

/**
 * Hook invoked when a connection joins a WebSocket group.
 * <p>
 * Purpose: react to group membership changes.
 * Trigger: invoked on group join.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should tolerate repeated joins.
 *
 * @param <J> the implementing type
 */
@FunctionalInterface
public interface GuicedWebSocketOnAddToGroup<J extends GuicedWebSocketOnAddToGroup<J>> extends IDefaultService<J> {
    /**
     * Handles group join events.
     *
     * @param groupName the group being joined
     * @return a future indicating completion
     */
    CompletableFuture<Boolean> onAddToGroup(String groupName);
}
