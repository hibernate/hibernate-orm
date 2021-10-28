/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class BatchBuilderImpl implements BatchBuilder, BatchBuilderMXBean {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BatchBuilderImpl.class );

	private volatile int jdbcBatchSize;

	/**
	 * Constructs a BatchBuilderImpl
	 */
	public BatchBuilderImpl() {
	}

	/**
	 * Constructs a BatchBuilderImpl
	 *
	 * @param jdbcBatchSize The batch jdbcBatchSize to use.
	 */
	public BatchBuilderImpl(int jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@Override
	public int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	@Override
	public void setJdbcBatchSize(int jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@Override
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		return SharedBatchBuildingCode.buildBatch( jdbcBatchSize, key, jdbcCoordinator );
	}

}
