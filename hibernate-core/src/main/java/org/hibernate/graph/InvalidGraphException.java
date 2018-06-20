/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

/**
 * Throw by {@link EntityGraphParser} to indicate textual entity graph representation parsing errors.
 * 
 * @author asusnjar
 *
 */
public class InvalidGraphException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidGraphException() {
	}

	public InvalidGraphException(String message) {
		super( message );
	}

	public InvalidGraphException(Throwable cause) {
		super( cause );
	}

	public InvalidGraphException(String message, Throwable cause) {
		super( message, cause );
	}

	public InvalidGraphException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super( message, cause, enableSuppression, writableStackTrace );
	}

}
