/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.Collections;
import java.util.function.Supplier;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcInsertMutation;

import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_LOGGER;
import static org.hibernate.jdbc.Expectations.NONE;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class BatchBuilderImpl implements BatchBuilder {
	private final int globalBatchSize;

	/**
	 * Constructs a BatchBuilderImpl
	 *
	 * @param globalBatchSize The batch size to use.  Can be overridden
	 * on {@link #buildBatch}
	 */
	public BatchBuilderImpl(int globalBatchSize) {
		if ( BATCH_LOGGER.isTraceEnabled() ) {
			BATCH_LOGGER.tracef(
					"Using standard BatchBuilder (%s)",
					globalBatchSize
			);
		}

		this.globalBatchSize = globalBatchSize;
	}

	public int getJdbcBatchSize() {
		return globalBatchSize;
	}

	@Override
	public Batch buildBatch(
			BatchKey key,
			Integer explicitBatchSize,
			Supplier<PreparedStatementGroup> statementGroupSupplier,
			JdbcCoordinator jdbcCoordinator) {
		final int batchSize = explicitBatchSize == null
				? globalBatchSize
				: explicitBatchSize;
		assert batchSize > 1;

		return new BatchImpl( key, statementGroupSupplier.get(), batchSize, jdbcCoordinator );
	}


	/**
	 * Intended for use from tests
	 */
	@Internal
	public BatchImpl buildBatch(BatchKey batchKey, Integer sizeOverride, String table, SessionImplementor session, String sql) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();

		final int batchSize = sizeOverride == null
				? globalBatchSize
				: sizeOverride;

		return new BatchImpl(
				batchKey,
				new PreparedStatementGroupSingleTable(
						new JdbcInsertMutation(
								new TableMapping() {
									@Override
									public String getTableName() {
										return table;
									}

									@Override
									public int getRelativePosition() {
										return 0;
									}

									@Override
									public KeyDetails getKeyDetails() {
										return null;
									}

									@Override
									public boolean isOptional() {
										return false;
									}

									@Override
									public boolean isInverse() {
										return false;
									}

									@Override
									public boolean isIdentifierTable() {
										return true;
									}

									@Override
									public MutationDetails getInsertDetails() {
										return null;
									}

									@Override
									public MutationDetails getUpdateDetails() {
										return null;
									}

									@Override
									public boolean isCascadeDeleteEnabled() {
										return false;
									}

									@Override
									public MutationDetails getDeleteDetails() {
										return null;
									}
								},
								null,
								sql,
								false,
								NONE,
								Collections.emptyList()
						),
						session
				),
				batchSize,
				jdbcCoordinator
		);
	}
}
