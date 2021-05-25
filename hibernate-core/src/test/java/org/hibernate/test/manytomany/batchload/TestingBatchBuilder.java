/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.manytomany.batchload;

import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.NonBatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

public class TestingBatchBuilder extends BatchBuilderImpl {
	@Override
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		return new TestingBatch( key, jdbcCoordinator );
	}

	public static class TestingBatch extends NonBatchingBatch {
		public TestingBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			super( key, jdbcCoordinator );
		}
	}
}

