/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.Query;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractSkipLockedTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class, BatchJob.class };
	}


	@Test
	@RequiresDialect(SQLServerDialect.class)
	@RequiresDialectFeature(DialectChecks.SupportsSkipLocked.class)
	public void testSQLServerSkipLocked() {

		doInHibernate( this::sessionFactory, session -> {
			for ( long i = 1; i <= 10; i++ ) {
				BatchJob batchJob = new BatchJob();
				batchJob.setId( i );
				session.persist( batchJob );
			}
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<BatchJob> firstFive = nextFiveBatchJobs( session );

			assertEquals( 5, firstFive.size() );
			assertTrue( firstFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
								.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeSync( () -> {
				doInHibernate( this::sessionFactory, _session -> {
					List<BatchJob> nextFive = nextFiveBatchJobs( _session );

					assertEquals( 5, nextFive.size() );

					assertTrue( nextFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
										.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
				} );
			} );

		} );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialectFeature(DialectChecks.SupportsSkipLocked.class)
	public void testPostgreSQLSkipLocked() {

		doInHibernate( this::sessionFactory, session -> {
			for ( long i = 1; i <= 10; i++ ) {
				BatchJob batchJob = new BatchJob();
				batchJob.setId( i );
				session.persist( batchJob );
			}
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<BatchJob> firstFive = nextFiveBatchJobs( session );

			assertEquals( 5, firstFive.size() );
			assertTrue( firstFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
								.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeSync( () -> {
				doInHibernate( this::sessionFactory, _session -> {
					List<BatchJob> nextFive = nextFiveBatchJobs( _session );

					assertEquals( 5, nextFive.size() );

					if ( lockMode() == LockMode.PESSIMISTIC_READ ) {
						assertTrue( nextFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
											.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );
					}
					else {
						assertTrue( nextFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
											.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
					}
				} );
			} );

		} );
	}

	@Test
	@RequiresDialect(OracleDialect.class)
	@RequiresDialectFeature(DialectChecks.SupportsSkipLocked.class)
	public void testOracleSkipLocked() {

		doInHibernate( this::sessionFactory, session -> {
			for ( long i = 1; i <= 10; i++ ) {
				BatchJob batchJob = new BatchJob();
				batchJob.setId( i );
				session.persist( batchJob );
			}
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<BatchJob> firstFive = nextFiveBatchJobs( session );

			assertEquals( 5, firstFive.size() );
			assertTrue( firstFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
								.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeSync( () -> {
				doInHibernate( this::sessionFactory, _session -> {
					List<BatchJob> nextFive = nextFiveBatchJobs( _session );

					assertEquals( 0, nextFive.size() );

					nextFive = nextFiveBatchJobs( _session, 10 );

					assertTrue( nextFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
										.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
				} );
			} );

		} );
	}

	@Test
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialectFeature(DialectChecks.SupportsSkipLocked.class)
	public void testMySQLSkipLocked() {

		doInHibernate( this::sessionFactory, session -> {
			for ( long i = 1; i <= 10; i++ ) {
				BatchJob batchJob = new BatchJob();
				batchJob.setId( i );
				session.persist( batchJob );
			}
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<BatchJob> firstFive = nextFiveBatchJobs( session );

			assertEquals( 5, firstFive.size() );
			assertTrue( firstFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
								.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeSync( () -> {
				doInHibernate( this::sessionFactory, _session -> {
					List<BatchJob> nextFive = nextFiveBatchJobs( _session );

					assertEquals( 5, nextFive.size() );

					if ( lockMode() == LockMode.PESSIMISTIC_READ ) {
						assertTrue( nextFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
											.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );
					}
					else {
						assertTrue( nextFive.stream().map( BatchJob::getId ).collect( Collectors.toList() )
											.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
					}
				} );
			} );

		} );
	}

	private List<BatchJob> nextFiveBatchJobs(Session session) {
		return nextFiveBatchJobs( session, 5 );
	}

	@SuppressWarnings("unchecked")
	private List<BatchJob> nextFiveBatchJobs(Session session, Integer maxResult) {
		Query query = session.createQuery(
				"select j from BatchJob j", BatchJob.class )
				.setMaxResults( maxResult )
				.unwrap( Query.class );

		applySkipLocked(query);

		return query.list();
	}

	protected void applySkipLocked(Query query) {
		query.setHibernateLockMode( lockMode() ).setTimeout( Timeout.milliseconds( -2 ) );
	}

	protected abstract LockMode lockMode();

	@Entity(name = "BatchJob")
	public static class BatchJob {

		@Id
		private Long id;

		private boolean processed;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean isProcessed() {
			return processed;
		}

		public void setProcessed(boolean processed) {
			this.processed = processed;
		}
	}
}
