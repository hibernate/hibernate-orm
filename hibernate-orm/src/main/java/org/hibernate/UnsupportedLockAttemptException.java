/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * This exception is thrown when an invalid LockMode is selected for an entity. This occurs if the
 * user tries to set an inappropriate LockMode for an entity.
 *
 * @author John O'Hara
 */
public class UnsupportedLockAttemptException extends HibernateException {
	public UnsupportedLockAttemptException(String message) {
		super( message );
	}

	public UnsupportedLockAttemptException(Throwable cause) {
		super( cause );
	}

	public UnsupportedLockAttemptException(String message, Throwable cause) {
		super( message, cause );
	}
}
