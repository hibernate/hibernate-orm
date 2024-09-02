/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when an invalid {@link LockMode} is requested for a given entity, in
 * particular, when a {@linkplain Session#setReadOnly(Object, boolean) read only}
 * entity is locked with a mode stricter than {@link LockMode#READ READ}.
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
