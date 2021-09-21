/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Used when a user provided type does not match the expected one.
 *
 * @author Emmanuel Bernard
 */
public class TypeMismatchException extends HibernateException {
	/**
	 * Constructs a TypeMismatchException using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TypeMismatchException(String message) {
		super( message );
	}
}
