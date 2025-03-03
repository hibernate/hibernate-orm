/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;


public final class ExceptionHelper {

	private ExceptionHelper() {
	}

	/**
	 * Throws the given throwable even if it is a checked exception.
	 *
	 * @param e The throwable to throw.
	 */
	public static void doThrow(Throwable e) {
		ExceptionHelper.doThrow0(e);
	}

	public static Throwable getRootCause(Throwable error) {
		Throwable toProcess = error;
		while ( toProcess.getCause() != null ) {
			toProcess = toProcess.getCause();
		}
		return toProcess;
	}

	public static <T extends Throwable> T combine(T throwable, T otherThrowable) {
		T toThrow = throwable;
		if ( otherThrowable != null ) {
			if ( toThrow != null ) {
				toThrow.addSuppressed( otherThrowable );
			}
			else {
				toThrow = otherThrowable;
			}
		}
		return toThrow;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void doThrow0(Throwable e) throws T {
		throw (T) e;
	}
}
