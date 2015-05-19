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
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.Configurable;

import org.jboss.logging.Logger;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class BatchBuilderImpl implements BatchBuilder, Configurable {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			BatchBuilderImpl.class.getName()
	);

	private int size;

	/**
	 * Constructs a BatchBuilderImpl
	 */
	public BatchBuilderImpl() {
	}

	/**
	 * Constructs a BatchBuilderImpl
	 *
	 * @param size The batch size to use.
	 */
	public BatchBuilderImpl(int size) {
		this.size = size;
	}

	@Override
	public void configure(Map configurationValues) {
		size = ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, size );
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setJdbcBatchSize(int size) {
		this.size = size;
	}

	@Override
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		LOG.tracef( "Building batch [size=%s]", size );
		return size > 1
				? new BatchingBatch( key, jdbcCoordinator, size )
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

