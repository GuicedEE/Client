package com.guicedee.client.services;

import com.google.inject.AbstractModule;

/**
 * Binder hook for registering internal GuicedEE services into an {@link AbstractModule}.
 *
 * @param <M> the module type to configure
 */
@SuppressWarnings("unused")
public interface IGuiceContextInternalBinder<M extends AbstractModule>
		extends IDefaultBinder<AbstractModule> {

}
