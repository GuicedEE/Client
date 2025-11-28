import com.guicedee.client.implementations.GuicedEEClientModule;
import com.guicedee.client.implementations.GuicedEEClientPostStartup;
import com.guicedee.client.implementations.GuicedEEClientStartup;
import com.guicedee.client.services.IGuiceProvider;
import com.guicedee.client.services.IJobServiceProvider;
import com.guicedee.client.services.lifecycle.*;
import com.guicedee.client.services.websocket.IWebSocketMessageReceiver;

module com.guicedee.client {
		requires transitive com.google.guice;
		requires transitive io.github.classgraph;
		requires transitive com.fasterxml.jackson.databind;
		
		//requires transitive jakarta.validation;
		
		exports com.guicedee.client;
		requires transitive io.vertx.core;
		
		requires transitive org.jspecify;
		
		requires static lombok;
		
		requires org.apache.commons.lang3;
		
		//slf4j config
		requires org.apache.logging.log4j.core;
		requires org.apache.logging.log4j.jul;
		requires org.apache.logging.log4j;
		requires org.apache.logging.log4j.slf4j2.impl;
		requires java.logging;
		//requires io.vertx;
		
		requires static jakarta.inject;
		requires io.smallrye.mutiny;
		
		uses IGuiceProvider;
		uses IJobServiceProvider;
		uses IOnCallScopeEnter;
		uses IOnCallScopeExit;
		uses IWebSocketMessageReceiver;
		
		opens com.guicedee.client.implementations to com.google.guice;
		opens com.guicedee.client to com.fasterxml.jackson.databind;
		exports com.guicedee.client.services;
		exports com.guicedee.client.implementations;
		exports com.guicedee.client.annotations;
		exports com.guicedee.client.utils;
		opens com.guicedee.client.utils to com.fasterxml.jackson.databind;
		exports com.guicedee.client.scopes;
		opens com.guicedee.client.scopes to com.fasterxml.jackson.databind;
		opens com.guicedee.client.services to com.google.guice;
		opens com.guicedee.client.annotations to com.google.guice;
		exports com.guicedee.client.services.websocket;
		opens com.guicedee.client.services.websocket to com.fasterxml.jackson.databind, com.google.guice;
		exports com.guicedee.client.services.lifecycle;
		opens com.guicedee.client.services.lifecycle to com.google.guice;
		opens com.guicedee.client.services.config to com.google.guice;
		exports com.guicedee.client.services.config;
		
		provides IGuicePreStartup with GuicedEEClientStartup;
		provides IGuicePostStartup with GuicedEEClientPostStartup;
		provides IGuiceModule with GuicedEEClientModule;
}