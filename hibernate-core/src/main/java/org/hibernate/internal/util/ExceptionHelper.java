/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		ExceptionHelper.<RuntimeException>doThrow0(e);
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
