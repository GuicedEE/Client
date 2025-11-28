package com.guicedee.client.services.lifecycle;

import com.google.inject.Scope;
import com.guicedee.client.services.IDefaultService;

@FunctionalInterface
public interface IOnCallScopeEnter<J extends IOnCallScopeEnter<J>> extends IDefaultService<J>
{
    void onScopeEnter(Scope scope);
}
