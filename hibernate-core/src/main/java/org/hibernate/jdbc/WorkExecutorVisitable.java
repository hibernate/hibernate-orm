/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This interface provides a way to execute unrelated "work" objects using
 * polymorphism.
 *
 * Instances of this interface can accept a {@link WorkExecutor} visitor
 * for executing a discrete piece of work, and return an implementation-defined
 * result.
 *
 * @author Gail Badner
 */
@FunctionalInterface
public interface WorkExecutorVisitable<T> {
	/**
	 * Accepts a {@link WorkExecutor} visitor for executing a discrete
	 * piece of work, and returns an implementation-defined result.
	 *
	 * @param executor The visitor that executes the work.
	 * @param connection The connection on which to perform the work.
	 *
	 * @return an implementation-defined result
	 *
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws org.hibernate.HibernateException Generally indicates a wrapped SQLException.
	 */
	T accept(WorkExecutor<T> executor, Connection connection) throws SQLException;

	/**
	 * In the case that this work was supposed to have been done in an
	 * isolated transaction, but the environment does not support
	 * transaction suspension, notify that we might need to leave some
	 * work in a provisional state until the transaction has completed.
	 * @since 7.3
	 */
	default void begin() {}
	/**
	 * In the case that this work was supposed to have been done in an
	 * isolated transaction, but the environment does not support
	 * transaction suspension, this method is called to make final any
	 * provisional work if the surrounding transaction ultimately succeeds.
	 * @see org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#supportsSuspension
	 * @since 7.3
	 */
	default void commit() {}
	/**
	 * In the case that this work was supposed to have been done in an
	 * isolated transaction, but the environment does not support
	 * transaction suspension, this method is called to perform any
	 * necessary compensating rollback operation, for example, to discard
	 * any provisional work, in the case that the surrounding transaction
	 * ultimately fails.
	 * @see org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#supportsSuspension
	 * @since 7.3
	 */
	default void rollback() {}
}
