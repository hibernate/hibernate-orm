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

/**
 * Simplified version of BatchBuilderImpl which does not support
 * changing the configured jdbc Batch size at runtime and is
 * not exposed via JMX.
 * @author Sanne Grinovero
 */
final class UnmodifiableBatchBuilderImpl implements BatchBuilder {

	private final int jdbcBatchSize;

	public UnmodifiableBatchBuilderImpl(int jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@Override
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		return SharedBatchBuildingCode.buildBatch( jdbcBatchSize, key, jdbcCoordinator );
	}

}
