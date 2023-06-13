/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * Indicates an attempt to use a path in an unsupported way
 *
 * @author Steve Ebersole
 *
 * @see PathElementException
 * @see TerminalPathException
 */
public class PathException extends SemanticException {
	public PathException(String message) {
		super( message );
	}

	/**
	 * @deprecated This is currently unused
	 */
	@Deprecated(forRemoval = true, since = "6.3")
	public PathException(String message, Exception cause) {
		super( message, cause );
	}
}
