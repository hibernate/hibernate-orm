/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Manageable;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class BatchBuilderImpl implements BatchBuilder, Configurable, Manageable, BatchBuilderMXBean {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BatchBuilderImpl.class );

	private int jdbcBatchSize;

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
	public void configure(Map configurationValues) {
		jdbcBatchSize = ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, jdbcBatchSize );
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
		final Integer sessionJdbcBatchSize = jdbcCoordinator.getJdbcSessionOwner()
				.getJdbcBatchSize();
		final int jdbcBatchSizeToUse = sessionJdbcBatchSize == null ?
				this.jdbcBatchSize :
				sessionJdbcBatchSize;
		return jdbcBatchSizeToUse > 1
				? new BatchingBatch( key, jdbcCoordinator, jdbcBatchSizeToUse )
				: new NonBatchingBatch( key, jdbcCoordinator );
	}

	@Override
	public String getManagementDomain() {
		// use Hibernate default domain
		return null;
	}

	@Override
	public String getManagementServiceType() {
		// use Hibernate default scheme
		return null;
	}

	@Override
	public Object getManagementBean() {
		return this;
	}
}
