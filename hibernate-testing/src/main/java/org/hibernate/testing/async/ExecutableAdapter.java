/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.async;

/**
 * @author Steve Ebersole
 */
public class ExecutableAdapter implements Runnable {
	private final Executable executable;
	private boolean isDone;
	private Throwable error;

	public ExecutableAdapter(Executable executable) {
		this.executable = executable;
	}

	public boolean isDone() {
		return isDone;
	}

	public void reThrowAnyErrors() {
		if ( error != null ) {
			if ( RuntimeException.class.isInstance( error ) ) {
				throw RuntimeException.class.cast( error );
			}
			else if ( Error.class.isInstance( error ) ) {
				throw Error.class.cast(  error );
			}
			else {
				throw new ExceptionWrapper( error );
			}
		}
	}

	@Override
	public void run() {
		isDone = false;
		error = null;
		try {
			executable.execute();
		}
		catch (Throwable t) {
			error = t;
		}
		finally {
			isDone = true;
		}
	}

	public static class ExceptionWrapper extends RuntimeException {
		public ExceptionWrapper(Throwable cause) {
			super( cause );
		}
	}
}
