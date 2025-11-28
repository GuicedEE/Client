package com.guicedee.client.services.lifecycle;

import com.guicedee.client.services.IDefaultService;

@FunctionalInterface
public interface IOnCallScopeExit<J extends IOnCallScopeExit<J>> extends IDefaultService<J>
{
    void onScopeExit();
}
