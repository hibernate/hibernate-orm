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

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.Configurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class BatchBuilderImpl implements BatchBuilder, Configurable {
	private static final Logger log = LoggerFactory.getLogger( BatchBuilderImpl.class );

	private int size;

	public BatchBuilderImpl() {
	}

	@Override
	public void configure(Map configurationValues) {
		size = ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, size );
	}

	public BatchBuilderImpl(int size) {
		this.size = size;
	}

	public void setJdbcBatchSize(int size) {
		this.size = size;
	}

	@Override
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		log.trace( "building batch [size={}]", size );
		return size > 1
				? new BatchingBatch( key, jdbcCoordinator, size )
				: new NonBatchingBatch( key, jdbcCoordinator );
	}

	@Override
	public String getManagementDomain() {
		return null; // use Hibernate default domain
	}

	@Override
	public String getManagementServiceType() {
		return null;  // use Hibernate default scheme
	}

	@Override
	public Object getManagementBean() {
		return this;
	}
}

