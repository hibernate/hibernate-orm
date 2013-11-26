/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
