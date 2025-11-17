/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
