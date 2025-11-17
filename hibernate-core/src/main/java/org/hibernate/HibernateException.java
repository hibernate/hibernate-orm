/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.PersistenceException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The base type for exceptions thrown by Hibernate.
 * <p>
 * Note that every {@linkplain java.sql.SQLException exception arising
 * from the JDBC layer} is wrapped in some form of {@link JDBCException}.
 *
 * @author Gavin King
 */
public class HibernateException extends PersistenceException {
	/**
	 * Constructs a {@code HibernateException} using the given exception message.
	 *
	 * @param message The message explaining the reason for the exception
	 */
	public HibernateException(String message) {
		super( message );
	}

	/**
	 * Constructs a {@code HibernateException} using the given message and underlying cause.
	 *
	 * @param cause The underlying cause.
	 */
	public HibernateException(@Nullable Throwable cause) {
		super( cause );
	}

	/**
	 * Constructs a {@code HibernateException} using the given message and underlying cause.
	 *
	 * @param message The message explaining the reason for the exception.
	 * @param cause The underlying cause.
	 */
	public HibernateException(String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
