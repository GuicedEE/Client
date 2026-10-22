module com.guicedee.inject.client.test {
    requires com.guicedee.client;
    requires org.junit.jupiter.api;
    requires com.google.guice;
    requires tools.jackson.databind;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires io.vertx.core;
    requires io.smallrye.mutiny;

    opens com.guicedee.client.test to org.junit.platform.commons;
}