package com.guicedee.client.scopes;

/**
 * Enumerates known sources that may enter a {@link CallScope}.
 */
public enum CallScopeSource
{
    /** Unspecified or unknown source. */
    Unknown,
    /** HTTP request entry. */
    Http,
    /** WebSocket event entry. */
    WebSocket,
    /** RabbitMQ consumer or publisher entry. */
    RabbitMQ,
    /** Scheduled timer entry. */
    Timer,
    /** Serial port event entry. */
    SerialPort,
    /** Transaction or unit-of-work entry. */
    Transaction,
    /** Test harness entry. */
    Test,
    /** REST-specific entry. */
    Rest,
    /** Persistence-layer entry. */
    Persistence,
    /** SOAP or service integration entry. */
    WebService,
    /** Application startup entry. */
    Startup,
    /** Vert.x consumer entry. */
    VertXConsumer,
    /** Vert.x producer entry. */
    VertXProducer,
    /** Event entry. */
    Event,
    /** Kafka consumer or producer entry. */
    Kafka
}
