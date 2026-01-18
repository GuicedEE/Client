/*
 * Copyright (C) 2017 GedMarc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.guicedee.client.services.lifecycle;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.IDefaultService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Post-startup lifecycle hook executed after the injector is ready.
 * <p>
 * Purpose: run initialization that depends on injected services.
 * Trigger: invoked immediately after injector creation.
 * Order: ascending {@link #sortOrder()}, default 50.
 * Idempotency: implementations should be safe to invoke once and tolerate repeated calls.
 *
 * @author GedMarc
 * @since 15 May 2017
 */
public interface IGuicePostStartup<J extends IGuicePostStartup<J>>
        extends IDefaultService<J>
{
    /**
     * Executes the post-startup logic.
     *
     * @return futures representing async startup work
     */
    List<Future<Boolean>> postLoad();

    /**
     * Executes a callable on the shared worker pool and returns its future.
     *
     * @param callable the work to execute
     * @param grouped whether the execution should be grouped
     * @return a future for the execution result
     */
    default Future<Boolean> executeSingle(Callable<Boolean> callable, boolean grouped)
    {
        Promise<Boolean> promise = Promise.promise();
        execute(callable, grouped)
                .onComplete(promise::complete, promise::fail);
        return promise.future();
    }

    /**
     * Executes a callable using a shared worker executor.
     *
     * @param callable the work to execute
     * @param grouped whether the execution should be grouped
     * @return a future for the execution result
     */
    default Future<Boolean> execute(Callable<Boolean> callable, boolean grouped)
    {
        Promise<Boolean> promise = Promise.promise();
        var executor = getVertx().createSharedWorkerExecutor("startup.worker.pool");
        executor.executeBlocking(callable, grouped)
                .onComplete(((result, failure) -> {
                    promise.complete(result, failure);
                }));
        return promise.future();
    }

    /**
     * Sets the order in which this must run, default 50.
     *
     * @return the sort order to return
     */
    @Override
    default Integer sortOrder()
    {
        return 50;
    }

    /**
     * Returns the Vert.x instance from the Guice context.
     *
     * @return the Vertx instance
     */
    default Vertx getVertx()
    {
        return IGuiceContext.get(Vertx.class);
    }

}
