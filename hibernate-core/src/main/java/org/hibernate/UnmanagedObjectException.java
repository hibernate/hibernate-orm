/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown when an operation requires a managed persistent
 * instance associated with the current persistence context
 * and encounters a transient or detached object instead.
 * Most instances of {@code UnmanagedObjectException} are
 * also instances of {@link TransientObjectException} or
 * {@link DetachedObjectException}.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
public class UnmanagedObjectException extends HibernateException {
	public UnmanagedObjectException(String message) {
		super( message );
	}
}
