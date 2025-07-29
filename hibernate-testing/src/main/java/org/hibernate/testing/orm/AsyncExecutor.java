/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Steve Ebersole
 */
public class AsyncExecutor {
	public static void executeAsync(Runnable action) {
		final ExecutorService executorService = Executors.newSingleThreadExecutor();
		try {
			executorService.submit( action ).get();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( "Thread interruption", e );
		}
		catch (ExecutionException e) {
			throw new RuntimeException( "Async execution error", e );
		}
	}

	public static void executeAsync(int timeout, TimeUnit timeoutUnit, Runnable action) {
		final ExecutorService executorService = Executors.newSingleThreadExecutor();
		try {
			executorService.submit( action ).get( timeout, timeoutUnit );
		}
		catch (InterruptedException e) {
			throw new TimeoutException( "Thread interruption", e );
		}
		catch (java.util.concurrent.TimeoutException e) {
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
