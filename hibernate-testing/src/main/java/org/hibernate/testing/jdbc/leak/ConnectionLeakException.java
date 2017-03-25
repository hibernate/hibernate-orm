/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc.leak;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionLeakException extends RuntimeException {

	public ConnectionLeakException() {
	}

	public ConnectionLeakException(String message) {
		super( message );
	}

	public ConnectionLeakException(String message, Throwable cause) {
		super( message, cause );
	}

	public ConnectionLeakException(Throwable cause) {
		super( cause );
	}

	public ConnectionLeakException(
			String message,
			Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super( message, cause, enableSuppression, writableStackTrace );
	}
}
