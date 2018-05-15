package org.hibernate.test.locking;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.query.Query;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

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
	@RequiresDialect({ SQLServer2005Dialect.class })
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
	@RequiresDialect({ PostgreSQL95Dialect.class })
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
	@RequiresDialect({ Oracle8iDialect.class })
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
	@RequiresDialect({ MySQL8Dialect.class })
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
		query.setLockOptions(
				new LockOptions( lockMode() )
						.setTimeOut( LockOptions.SKIP_LOCKED )
		);
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
