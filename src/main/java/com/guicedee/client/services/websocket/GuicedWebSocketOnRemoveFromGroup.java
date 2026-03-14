package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;

import java.util.concurrent.CompletableFuture;

/**
 * Hook invoked when a connection leaves a WebSocket group.
 * <p>
 * Purpose: react to group membership removal.
 * Trigger: invoked on group leave.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should tolerate repeated removals.
 *
 * @param <J> the implementing type
 */
public interface GuicedWebSocketOnRemoveFromGroup<J extends GuicedWebSocketOnRemoveFromGroup<J>>  extends IDefaultService<J> {
    /**
     * Handles group leave events.
     *
     * @param groupName the group being left
     * @return a future indicating completion
     */
    CompletableFuture<Boolean> onRemoveFromGroup(String groupName);
}
