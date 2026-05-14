/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;
import org.hibernate.sql.model.PreparableMutationOperation;

/**
 * A builder for {@link Batch} instances.
 * <p>
 * The core batch lifecycle is split into specializations.  Implementors should
 * create a {@link GroupedBatch} for the general mutation executor path, and may
 * additionally create a {@link SingleStatementBatch} for execution paths that
 * bind directly to one statement shape.
 * <p>
 * A custom {@code BatchBuilder} may be selected either by setting the configuration
 * property {@value org.hibernate.cfg.AvailableSettings#BUILDER}, or by registering it
 * as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 */
@Incubating
@JavaServiceLoadable
public interface BatchBuilder extends Service {
	/**
	 * Build a batch for the general mutation executor infrastructure.
	 * <p>
	 * The returned batch owns the {@link PreparedStatementGroup} produced by
	 * {@code statementGroupSupplier}.  The supplier is passed instead of an eagerly
	 * created group so implementations may decide exactly when statement-group
	 * construction should happen.
	 *
	 * @param key key identifying compatible rows for the batch
	 * @param batchSize explicit batch size override, or {@code null} to use the
	 * builder's default
	 * @param statementGroupSupplier supplier for the statement group owned by the batch
	 * @param jdbcCoordinator JDBC coordinator that will own the active batch
	 *
	 * @return a grouped batch instance
	 */
	GroupedBatch buildGroupedBatch(
			BatchKey key,
			Integer batchSize,
			Supplier<PreparedStatementGroup> statementGroupSupplier,
			JdbcCoordinator jdbcCoordinator);

	/**
	 * Build a batch for a single JDBC statement shape.
	 * <p>
	 * The default implementation throws because existing custom builders written
	 * for grouped mutation batching do not necessarily know how to build this
	 * specialization.  Builders that want to support graph-style single statement
	 * batching should override this method.
	 *
	 * @param key key identifying compatible rows for the batch
	 * @param batchSize explicit batch size override, or {@code null} to use the
	 * builder's default
	 * @param mutationOperation preparable mutation operation representing the
	 * single statement shape
	 * @param jdbcCoordinator JDBC coordinator that will own the active batch
	 *
	 * @return a single-statement batch instance
	 */
	default SingleStatementBatch buildSingleStatementBatch(
			BatchKey key,
			Integer batchSize,
			PreparableMutationOperation mutationOperation,
			JdbcCoordinator jdbcCoordinator) {
		throw new UnsupportedOperationException(
				"BatchBuilder does not support single-statement batches: " + getClass().getName()
		);
	}
}
