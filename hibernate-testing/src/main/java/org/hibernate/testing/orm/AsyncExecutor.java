/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Steve Ebersole
 */
public class AsyncExecutor {

	// Need more than a single thread, because not all databases support cancellation of statements waiting for locks
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

	public static void executeAsync(Runnable action) {
		final Future<?> future = EXECUTOR_SERVICE.submit( action );
		try {
			future.get();
		}
		catch (InterruptedException e) {
			future.cancel( true );
			throw new TimeoutException( "Thread interruption", e );
		}
		catch (ExecutionException e) {
			throw new RuntimeException( "Async execution error", e.getCause() );
		}
	}

	public static void executeAsync(int timeout, TimeUnit timeoutUnit, Runnable action) {
		final Future<?> future = EXECUTOR_SERVICE.submit( action );
		try {
			future.get( timeout, timeoutUnit );
		}
		catch (InterruptedException e) {
			future.cancel( true );
			throw new TimeoutException( "Thread interruption", e );
		}
		catch (java.util.concurrent.TimeoutException e) {
			future.cancel( true );
			throw new TimeoutException( "Thread timeout exceeded", e );
		}
		catch (ExecutionException e) {
			throw new RuntimeException( "Async execution error", e.getCause() );
		}
	}

	public static class TimeoutException extends RuntimeException {
		public TimeoutException(String message) {
			super( message );
		}

		public TimeoutException(String message, Throwable cause) {
			super( message, cause );
		}
	}
}
