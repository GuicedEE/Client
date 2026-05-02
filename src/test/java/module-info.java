module com.guicedee.inject.client.test {
    requires com.guicedee.client;
    requires org.junit.jupiter.api;
    requires com.google.guice;
    requires com.fasterxml.jackson.databind;
    requires io.vertx.core;
    requires io.smallrye.mutiny;

    opens com.guicedee.client.test to org.junit.platform.commons;
}