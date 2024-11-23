/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

/**
 * Common code across BatchBuilder service implementors
 */
final class SharedBatchBuildingCode {

	static Batch buildBatch(final int defaultJdbcBatchSize, final BatchKey key, final JdbcCoordinator jdbcCoordinator) {
		final Integer sessionJdbcBatchSize = jdbcCoordinator.getJdbcSessionOwner()
				.getJdbcBatchSize();
		final int jdbcBatchSizeToUse = sessionJdbcBatchSize == null ?
				defaultJdbcBatchSize :
				sessionJdbcBatchSize;
		return jdbcBatchSizeToUse > 1
				? new BatchingBatch( key, jdbcCoordinator, jdbcBatchSizeToUse )
				: new NonBatchingBatch( key, jdbcCoordinator );
	}
}
