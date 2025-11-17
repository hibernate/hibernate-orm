/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = { BatchSizeAndStatelessSessionTest.TestEntity.class }
)
@SessionFactory
@JiraKey( value = "HHH-16249")
public class BatchSizeAndStatelessSessionTest {

	private final String countQuery = "select count(id) from TestEntity";
	private final int batchSize = 3;
	private final int total = 10;

	@AfterEach
	public void cleanup( SessionFactoryScope scope ) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBatchWithStatelessSessionTx( SessionFactoryScope scope ) {
			scope.inStatelessTransaction(
					ss -> {
						SelectionQuery<Long> query = ss.createSelectionQuery( countQuery, Long.class );
						ss.setJdbcBatchSize( batchSize );
						long intermediateCount = 0;
						for ( int i = 1; i <= total; i++ ) {
							ss.insert( new TestEntity(i) );
							long count = query.getSingleResult();
							// This should be batched, so the count should remain 0 or a multiple of the batch size and only change
							// when a batch is executed
							if ( i % batchSize == 0 ) {
								assertEquals( i, count );
								intermediateCount += batchSize;
							} else {
								assertEquals( intermediateCount, count );
							}
						}
					}
			);

		checkTotal( scope );
	}

	@Test
	public void testBatchWithStatelessSessionNoTx( SessionFactoryScope scope ) {
		scope.inStatelessSession(
				ss -> {
					ss.setJdbcBatchSize( batchSize );
					SelectionQuery<Long> query = ss.createSelectionQuery( countQuery, Long.class );
					for ( int i = 1; i <= total; i++ ) {
						ss.insert( new TestEntity( i ) );
						long count = query.getSingleResult();
						// There shouldn't be any batching here, so the count should go up one at a time
						assertEquals( i, count );
					}
				}
		);

		checkTotal( scope );
	}

	@Test
	public void testBatchWithStatelessSessionInParentTx( SessionFactoryScope scope ) {
		scope.inSession(
				s -> {
					s.beginTransaction();
					try (StatelessSession ss = s.getSessionFactory().openStatelessSession()) {
						SelectionQuery<Long> query = ss.createSelectionQuery( countQuery, Long.class );
						ss.setJdbcBatchSize(batchSize);
						for ( int i = 1; i <= total; i++ ) {
							ss.insert( new TestEntity(i) );
							long count = query.getSingleResult();
							// Even though it's inside a parent Tx, there's no batching here, so the count should go up one at a time
							assertEquals( i, count );
						}
					}
					s.getTransaction().commit();
				}
		);

		checkTotal( scope );
	}

	private void checkTotal(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					SelectionQuery<Long> q = s.createSelectionQuery( countQuery, Long.class );
					assertEquals( total, q.getSingleResult() );
				}
		);
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		int id;

		public TestEntity() {
		}

		public TestEntity( int id ) {
			this.id = id;
		}
	}
}
