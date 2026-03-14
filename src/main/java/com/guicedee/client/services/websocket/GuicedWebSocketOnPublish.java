package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;

/**
 * Hook invoked when a message is published to a WebSocket group.
 * <p>
 * Purpose: react to or intercept publish events.
 * Trigger: invoked during broadcast operations.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should tolerate repeated publish attempts.
 *
 * @param <J> the implementing type
 */
@FunctionalInterface
public interface GuicedWebSocketOnPublish<J extends GuicedWebSocketOnPublish<J>> extends IDefaultService<J> {
    /**
     * Publishes a message to the given group.
     *
     * @param groupName the target group name
     * @param message the message payload
     * @return true if the publish is already complete
     * @throws Exception when the publish fails
     */
    boolean publish(String groupName,String message) throws Exception;
}
