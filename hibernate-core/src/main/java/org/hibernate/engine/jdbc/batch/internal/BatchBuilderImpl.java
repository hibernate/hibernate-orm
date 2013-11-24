/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

