package com.guicedee.client.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.scopes.CallScope;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScoper;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import lombok.extern.log4j.Log4j2;

/**
 * Guice module that registers call scope bindings for the client.
 */
@Log4j2
public class GuicedEEClientModule extends AbstractModule implements IGuiceModule<GuicedEEClientModule>
{
		/**
		 * Creates a new client module for call scope registration.
		 */
		public GuicedEEClientModule() {
		}

		/**
		 * Configures call scope bindings.
		 */
		@Override
		protected void configure()
		{
				bindScope(CallScope.class, new CallScoper());
				bind(CallScopeProperties.class).in(CallScope.class);
		}
}
