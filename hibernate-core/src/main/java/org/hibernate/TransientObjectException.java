/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when the user passes a transient instance to a Session method that expects a persistent instance.
 *
 * @author Gavin King
 */
public class TransientObjectException extends HibernateException {
	/**
	 * Constructs a TransientObjectException using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TransientObjectException(String message) {
		super( message );
	}

}
