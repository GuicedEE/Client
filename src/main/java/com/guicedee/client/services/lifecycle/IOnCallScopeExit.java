package com.guicedee.client.services.lifecycle;

import com.guicedee.client.services.IDefaultService;

/**
 * Lifecycle hook invoked when a {@link com.guicedee.client.scopes.CallScope} is exited.
 * <p>
 * Purpose: cleanup scope-specific resources.
 * Trigger: called on scope exit from {@link com.guicedee.client.scopes.CallScoper#exit()}.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should tolerate repeated invocations per scope.
 *
 * @param <J> the implementing scope-exit type
 */
@FunctionalInterface
public interface IOnCallScopeExit<J extends IOnCallScopeExit<J>> extends IDefaultService<J>
{
    /**
     * Executes when a call scope is exited.
     */
    void onScopeExit();
}
