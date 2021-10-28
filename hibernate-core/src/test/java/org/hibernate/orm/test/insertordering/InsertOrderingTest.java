/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.insertordering;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/insertordering/Mapping.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = {@Setting( name = Environment.ORDER_INSERTS, value = "true"),
				@Setting( name = Environment.STATEMENT_BATCH_SIZE, value = "10"),
				@Setting( name = BatchBuilderInitiator.BUILDER, value = "org.hibernate.orm.test.insertordering.InsertOrderingTest$StatsBatchBuilder" )
		}
)
public class InsertOrderingTest {

	@Test
	public void testBatchOrdering(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int iterations = 12;
					for ( int i = 0; i < iterations; i++ ) {
						User user = new User( "user-" + i );
						Group group = new Group( "group-" + i );
						session.save( user );
						session.save( group );
						user.addMembership( group );
					}
					StatsBatch.reset();
				}
		);


		assertEquals( 3, StatsBatch.batchSizes.size() );

		scope.inTransaction(
				session -> {
					Iterator users = session.createQuery(
							"from User u left join fetch u.memberships m left join fetch m.group" ).list().iterator();
					while ( users.hasNext() ) {
						session.delete( users.next() );
					}
				}
		);
	}

	public static class Counter {
		public int count = 0;
	}

	public static class StatsBatch extends BatchingBatch {
		private static String batchSQL;
		private static List batchSizes = new ArrayList();
		private static int currentBatch = -1;

		public StatsBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int jdbcBatchSize) {
			super( key, jdbcCoordinator, jdbcBatchSize );
		}

		static void reset() {
			batchSizes = new ArrayList();
			currentBatch = -1;
			batchSQL = null;
		}

		@Override
		public PreparedStatement getBatchStatement(String sql, boolean callable) {
			if ( batchSQL == null || !batchSQL.equals( sql ) ) {
				currentBatch++;
				batchSQL = sql;
				batchSizes.add( currentBatch, new Counter() );
			}
			return super.getBatchStatement( sql, callable );
		}

		@Override
		public void addToBatch() {
			Counter counter = (Counter) batchSizes.get( currentBatch );
			counter.count++;
			super.addToBatch();
		}
	}

	public static class StatsBatchBuilder extends BatchBuilderImpl {

		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return new StatsBatch( key, jdbcCoordinator, getJdbcBatchSize() );
		}
	}
}
