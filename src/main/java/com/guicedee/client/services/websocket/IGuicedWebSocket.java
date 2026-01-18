package com.guicedee.client.services.websocket;

import com.guicedee.client.IGuiceContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contract for broadcasting messages and managing groups for GuicedEE WebSocket support.
 */
public interface IGuicedWebSocket
{
    /**
     * Global registry of message listeners keyed by message name.
     */
    Map<String, IWebSocketMessageReceiver<?,?>> messageListeners = new ConcurrentHashMap<>();

    /**
     * Default group name that represents all listeners.
     */
    String EveryoneGroup = "Everyone";

    /**
     * Adds this connection to a group.
     *
     * @param groupName the group name to join
     * @throws Exception if the group could not be joined
     */
    void addToGroup(String groupName) throws Exception;

    /**
     * Removes this connection from a group.
     *
     * @param groupName the group name to leave
     * @throws Exception if the group could not be left
     */
    void removeFromGroup(String groupName) throws Exception;

    /**
     * Broadcasts a message to a specific group.
     *
     * @param groupName the target group name
     * @param message the message to send
     */
    void broadcastMessage(String groupName, String message);

    /**
     * Broadcasts a message to all listeners.
     *
     * @param message the message to send
     */
    void broadcastMessage(String message);

    /**
     * Broadcasts a message synchronously to a group.
     *
     * @param groupName the target group name
     * @param message the message to send
     * @throws Exception when the broadcast fails
     */
    void broadcastMessageSync(String groupName, String message) throws Exception;



    /**
     * Registers a receiver for all of its supported message names.
     *
     * @param receiver the receiver instance
     */
    static void addWebSocketMessageReceiver(IWebSocketMessageReceiver<?,?> receiver)
    {
        synchronized ("Websocket"){
        for (String messageName : receiver.messageNames())
        {
            addReceiver(receiver, messageName);
        }
    }
    }

    /**
     * Checks whether a receiver is registered for the given name.
     *
     * @param name the message name
     * @return true if a receiver is registered
     */
    static boolean isWebSocketReceiverRegistered(String name)
    {
        return messageListeners
                .containsKey(name);
    }

    /**
     * Guards against recursive receiver loading.
     */
    ThreadLocal<Boolean> loadingReceivers = ThreadLocal.withInitial(() -> false);

    /**
     * Registers a receiver for a specific message action.
     *
     * @param messageReceiver the receiver instance
     * @param action the action name to register
     */
    static void addReceiver(IWebSocketMessageReceiver<?,?> messageReceiver, String action)
    {
        if (messageListeners
                .isEmpty() && !loadingReceivers.get())
        {
            loadingReceivers.set(true);
            try {
                // Ensure the actual instance is tracked for subsequent loads
                IGuiceContext.loaderToSet(ServiceLoader.load(IWebSocketMessageReceiver.class))
                             .add(messageReceiver);
                loadWebSocketReceivers();
            } finally {
                loadingReceivers.set(false);
            }
        }
        // Register the actual instance so any configured properties are preserved
        messageListeners
                .put(action, messageReceiver);
    }

    /**
     * Returns the current registry of message listeners, loading if required.
     *
     * @return the message listener map
     */
    static Map<String, IWebSocketMessageReceiver<?,?>> getMessagesListeners()
    {
        if (messageListeners
                .isEmpty() && !loadingReceivers.get())
        {
            loadWebSocketReceivers();
        }
        return messageListeners;
    }

    /**
     * Loads message receivers using {@link ServiceLoader} and registers them.
     */
    static void loadWebSocketReceivers()
    {
        Set<IWebSocketMessageReceiver> messageReceivers = IGuiceContext.loaderToSet(ServiceLoader.load(IWebSocketMessageReceiver.class));
        for (IWebSocketMessageReceiver<?,?> messageReceiver : messageReceivers)
        {
            for (String s : messageReceiver.messageNames())
            {
                if (!IGuicedWebSocket.isWebSocketReceiverRegistered(s))
                {
                    IGuicedWebSocket.addReceiver(messageReceiver, s);
                }
            }
        }
    }
}
