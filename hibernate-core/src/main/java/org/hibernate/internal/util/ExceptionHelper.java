/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;


public final class ExceptionHelper {

	private ExceptionHelper() {
	}

	/**
	 * Throws the given {@link Throwable}, even if it's a checked exception.
	 *
	 * @param throwable The {@code Throwable} to throw.
	 */
	public static void rethrow(Throwable throwable) {
		sneakyThrow( throwable );
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void sneakyThrow(Throwable e)
			throws T {
		throw (T) e;
	}

	public static Throwable getRootCause(Throwable error) {
		var next = error;
		while ( true ) {
			final var current = next;
			next = current.getCause();
			if ( next == null ) {
				return current;
			}
		}
	}
}
