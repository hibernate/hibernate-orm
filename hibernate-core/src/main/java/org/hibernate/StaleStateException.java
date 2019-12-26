/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when a version number or timestamp check failed, indicating that the Session contained
 * stale data (when using long transactions with versioning). Also occurs if we try to delete or update
 * a row that does not exist.
 *
 * Note that this exception often indicates that the user failed to specify the correct
 * {@code unsaved-value} strategy for an entity
 *
 * @author Gavin King
 */
public class StaleStateException extends HibernateException {
	/**
	 * Constructs a StaleStateException using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public StaleStateException(String message) {
		super( message );
	}
}
