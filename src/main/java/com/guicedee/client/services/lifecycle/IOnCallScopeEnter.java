package com.guicedee.client.services.lifecycle;

import com.google.inject.Scope;
import com.guicedee.client.services.IDefaultService;

/**
 * Lifecycle hook invoked when a {@link com.guicedee.client.scopes.CallScope} is entered.
 * <p>
 * Purpose: initialize scope-specific resources.
 * Trigger: called on scope entry from {@link com.guicedee.client.scopes.CallScoper#enter()}.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should tolerate repeated invocations per scope.
 *
 * @param <J> the implementing scope-enter type
 */
@FunctionalInterface
public interface IOnCallScopeEnter<J extends IOnCallScopeEnter<J>> extends IDefaultService<J>
{
    /**
     * Executes when a call scope is entered.
     *
     * @param scope the active scope instance
     */
    void onScopeEnter(Scope scope);
}
