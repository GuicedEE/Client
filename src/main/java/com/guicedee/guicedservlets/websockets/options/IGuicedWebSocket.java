package com.guicedee.guicedservlets.websockets.options;

import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.websockets.services.IWebSocketMessageReceiver;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface IGuicedWebSocket
{
    Map<String, IWebSocketMessageReceiver<?,?>> messageListeners = new ConcurrentHashMap<>();

    String EveryoneGroup = "Everyone";

    void addToGroup(String groupName) throws Exception;

    void removeFromGroup(String groupName) throws Exception;

    void broadcastMessage(String groupName, String message);

    void broadcastMessage(String message);

    void broadcastMessageSync(String groupName, String message) throws Exception;



    static void addWebSocketMessageReceiver(IWebSocketMessageReceiver<?,?> receiver)
    {
        synchronized ("Websocket"){
        for (String messageName : receiver.messageNames())
        {
            addReceiver(receiver, messageName);
        }
    }
    }

    static boolean isWebSocketReceiverRegistered(String name)
    {
        return messageListeners
                .containsKey(name);
    }

    ThreadLocal<Boolean> loadingReceivers = ThreadLocal.withInitial(() -> false);

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

    static Map<String, IWebSocketMessageReceiver<?,?>> getMessagesListeners()
    {
        if (messageListeners
                .isEmpty() && !loadingReceivers.get())
        {
            loadWebSocketReceivers();
        }
        return messageListeners;
    }

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
