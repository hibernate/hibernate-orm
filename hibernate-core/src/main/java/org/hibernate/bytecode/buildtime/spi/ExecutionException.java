/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.spi;

/**
 * Indicates problem performing the instrumentation execution.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public class ExecutionException extends RuntimeException {
	/**
	 * Constructs an ExecutionException.
	 *
	 * @param message The message explaining the exception condition
	 */
	public ExecutionException(String message) {
		super( message );
	}

	/**
	 * Constructs an ExecutionException.
	 *
	 * @param cause The underlying cause.
	 */
	public ExecutionException(Throwable cause) {
		super( cause );
	}

	/**
	 * Constructs an ExecutionException.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause.
	 */
	public ExecutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
