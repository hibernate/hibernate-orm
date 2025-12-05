/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.function.Supplier;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcInsertMutation;

import static java.util.Collections.emptyList;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_MESSAGE_LOGGER;

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
		if ( globalBatchSize > 1 ) {
			BATCH_MESSAGE_LOGGER.batchingEnabled( globalBatchSize );
		}
		BATCH_MESSAGE_LOGGER.usingStandardBatchBuilder();
		this.globalBatchSize = globalBatchSize;
	}

	public int getJdbcBatchSize() {
		return globalBatchSize;
	}

	private int batchSize(Integer explicitBatchSize) {
		return explicitBatchSize == null
				? globalBatchSize
				: explicitBatchSize;
	}

	@Override
	public Batch buildBatch(
			BatchKey key,
			Integer explicitBatchSize,
			Supplier<PreparedStatementGroup> statementGroupSupplier,
			JdbcCoordinator jdbcCoordinator) {
		final int batchSize = batchSize( explicitBatchSize );
		assert batchSize > 1;
		return new BatchImpl( key, statementGroupSupplier.get(), batchSize, jdbcCoordinator );
	}

	/**
	 * Intended for use from tests
	 */
	@Internal
	public BatchImpl buildBatch(BatchKey batchKey, Integer sizeOverride, String table, SessionImplementor session, String sql) {
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
								Expectation.None.INSTANCE,
								emptyList()
						),
						session
				),
				batchSize( sizeOverride ),
				session.getJdbcCoordinator()
		);
	}
}
