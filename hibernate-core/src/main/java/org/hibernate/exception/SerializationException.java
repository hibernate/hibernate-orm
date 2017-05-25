/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception;

import org.hibernate.HibernateException;

/**
 * Thrown when an object cannot be serialized or deserialized.
 *
 * @author Chris Cranford
 */
public class SerializationException extends HibernateException {
	public SerializationException(String message, Throwable root) {
		super( message, root );
	}
}
