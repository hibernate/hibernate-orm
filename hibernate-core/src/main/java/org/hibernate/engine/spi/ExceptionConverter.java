/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockOptions;

/**
 * @author Andrea Boriero
 */
public interface ExceptionConverter {
	/**
	 * Converts the exception thrown during the transaction commit phase
	 *
	 * @param e The exception being handled
	 *
	 * @return The converted exception
	 */
	RuntimeException convertCommitException(RuntimeException e);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception;
	 * note that the JPA specification makes use of exceptions outside its
	 * exception hierarchy, though they are all runtime exceptions.
	 *
	 * @param e The Hibernate exception.
	 * @param lockOptions The lock options in effect at the time of exception (can be null)
	 *
	 * @return The JPA-specified exception
	 */
	RuntimeException convert(HibernateException e, LockOptions lockOptions);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception;
	 * note that the JPA specification makes use of exceptions outside its
	 * exception hierarchy, though they are all runtime exceptions.
	 *
	 * @param e The Hibernate exception.
	 *
	 * @return The JPA-specified exception
	 */
	RuntimeException convert(HibernateException e);

	RuntimeException convert(RuntimeException e);

	RuntimeException convert(RuntimeException e, LockOptions lockOptions);

	JDBCException convert(SQLException e, String message);
}
