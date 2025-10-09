/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.Timeouts;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.LockMode.UPGRADE_SKIPLOCKED;
import static org.hibernate.testing.orm.AsyncExecutor.executeAsync;

/**
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DomainModel(annotatedClasses = { A.class, SkipLockedTests.BatchJob.class })
@SessionFactory
public class SkipLockedTests {
	static List<Object[]> parameters() {
		return List.of(
				new Object[] { PESSIMISTIC_READ, null },
				new Object[] { PESSIMISTIC_WRITE, null },
				new Object[] { UPGRADE_SKIPLOCKED, FALSE }
		);
	}

	private final LockMode lockMode;
	private final Boolean followOnLocking;

	public SkipLockedTests(LockMode lockMode, Boolean followOnLocking) {
		this.lockMode = lockMode;
		this.followOnLocking = followOnLocking;
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			for ( long i = 1; i <= 10; i++ ) {
				var batchJob = new BatchJob();
				batchJob.setId( i );
				session.persist( batchJob );
			}
		} );
	}

	@Test
	@RequiresDialect(SQLServerDialect.class)
	@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsSkipLocked.class)
	public void testSQLServerSkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var firstFive = nextFiveBatchJobs( session );

			Assertions.assertEquals( 5, firstFive.size() );
			Assertions.assertTrue( firstFive.stream().map( BatchJob::getId ).toList()
					.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeAsync( () -> factoryScope.inTransaction( (_session) -> {
				var nextFive = nextFiveBatchJobs( _session );
				Assertions.assertEquals( 5, nextFive.size() );
				Assertions.assertTrue( nextFive.stream().map( BatchJob::getId ).toList()
						.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
			} ) );
		} );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	public void testPostgreSQLSkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var firstFive = nextFiveBatchJobs( session );
			Assertions.assertEquals( 5, firstFive.size() );
			Assertions.assertTrue( firstFive.stream().map( BatchJob::getId ).toList()
					.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeAsync( () -> factoryScope.inTransaction( (_session) -> {
				var nextFive = nextFiveBatchJobs( _session );
				Assertions.assertEquals( 5, nextFive.size() );

				if ( lockMode == PESSIMISTIC_READ ) {
					Assertions.assertTrue( nextFive.stream().map( BatchJob::getId ).toList()
							.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );
				}
				else {
					Assertions.assertTrue( nextFive.stream().map( BatchJob::getId ).toList()
							.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
				}
			} ) );
		} );
	}

	@Test
	@RequiresDialect(OracleDialect.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	public void testOracleSkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var firstFive = nextFiveBatchJobs( session );
			Assertions.assertEquals( 5, firstFive.size() );
			Assertions.assertTrue( firstFive.stream().map( BatchJob::getId ).toList()
					.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeAsync( () -> factoryScope.inTransaction( (_session) -> {
				var nextFive = nextFiveBatchJobs( _session );
				Assertions.assertEquals( 0, nextFive.size() );

				nextFive = nextFiveBatchJobs( _session, 10 );
				Assertions.assertTrue( nextFive.stream().map( BatchJob::getId ).toList()
						.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
			} ) );
		} );
	}

	@Test
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	public void testMySQLSkipLocked(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var firstFive = nextFiveBatchJobs( session );
			Assertions.assertEquals( 5, firstFive.size() );
			Assertions.assertTrue( firstFive.stream().map( BatchJob::getId ).toList()
					.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );

			executeAsync( () -> factoryScope.inTransaction( (_session) -> {
				var nextFive = nextFiveBatchJobs( _session );
				Assertions.assertEquals( 5, nextFive.size() );

				if ( lockMode == PESSIMISTIC_READ ) {
					Assertions.assertTrue( nextFive.stream().map( BatchJob::getId ).toList()
							.containsAll( Arrays.asList( 1L, 2L, 3L, 4L, 5L ) ) );
				}
				else {
					Assertions.assertTrue( nextFive.stream().map( BatchJob::getId ).toList()
							.containsAll( Arrays.asList( 6L, 7L, 8L, 9L, 10L ) ) );
				}
			} ) );
		} );
	}

	private List<BatchJob> nextFiveBatchJobs(Session session) {
		return nextFiveBatchJobs( session, 5 );
	}

	@SuppressWarnings("unchecked")
	private List<BatchJob> nextFiveBatchJobs(Session session, Integer maxResult) {
		var query = session.createQuery(
				"select j from BatchJob j", BatchJob.class )
				.setMaxResults( maxResult )
				.unwrap( Query.class );

		applySkipLocked(query);

		return query.list();
	}

	protected void applySkipLocked(Query<?> query) {
		query.setHibernateLockMode( lockMode )
				.setTimeout( Timeouts.SKIP_LOCKED );
		if ( followOnLocking != null ) {
			query.setFollowOnStrategy( followOnLocking ? Locking.FollowOn.FORCE : Locking.FollowOn.IGNORE );
		}
	}

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
