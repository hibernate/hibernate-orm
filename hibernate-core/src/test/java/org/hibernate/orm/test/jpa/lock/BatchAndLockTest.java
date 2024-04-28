package org.hibernate.orm.test.jpa.lock;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				BatchAndLockTest.TestEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2")
		}
)
@SessionFactory(
		useCollectingStatementInspector = true
)
@JiraKey("HHH-16820")
@RequiresDialect(H2Dialect.class)
public class BatchAndLockTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity1 = new TestEntity( 1, "entity 1" );
					TestEntity entity2 = new TestEntity( 2, "entity 2" );
					TestEntity entity3 = new TestEntity( 3, "entity 3" );
					TestEntity entity4 = new TestEntity( 4, "entity 4" );

					session.persist( entity1 );
					session.persist( entity2 );
					session.persist( entity3 );
					session.persist( entity4 );
				}
		);
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createMutationQuery( "delete from TestEntity" ).executeUpdate()
		);
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					assertEntityLockMode( testEntity, LockModeType.NONE, session );
				}
		);
		assertNoLokIsApplied( scope );
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					TestEntity testEntity = session.find( TestEntity.class, 1 );
					assertEntityLockMode( testEntity, LockModeType.NONE, session );

					// batching is enabled because LockMode = NONE, so the proxy has been initialized
					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEntityLockMode( proxy, LockModeType.NONE, session );
				}
		);
		assertNoLokIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModeNone(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.NONE );
					assertEntityLockMode( testEntity, LockModeType.NONE, session );
				}
		);
		assertNoLokIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModeNone2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.NONE );
					assertEntityLockMode( testEntity, LockModeType.NONE, session );

					// batching is enabled because LockMode = NONE, so the proxy has been initialized
					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEntityLockMode( proxy, LockModeType.NONE, session );
				}
		);
		assertNoLokIsApplied( scope );
	}

	private void assertNoLokIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );

		statementInspector.assertIsSelect( 0 );
		assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
	}

	@Test
	public void testFindApplyLockModeRead(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.READ );
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC, session );
				}
		);
		assertReadLockIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModeRead2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.READ );
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC, session );

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertReadLockIsApplied( scope );
	}

	private void assertReadLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsSelect( 0 );
		assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );

		assertVersionIsSelected( statementInspector, 1 );
	}

	@Test
	public void testFindApplyLockModeWrite(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.WRITE );
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT, session );
				}
		);
		assertWriteLockIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModeWrite2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.WRITE );
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT, session );

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertWriteLockIsApplied( scope );
	}

	private void assertWriteLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsSelect( 0 );
		assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );

		assertVersionIsUpdated( statementInspector, 1 );
	}

	@Test
	public void testFindApplyLockModeOptimistic(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.OPTIMISTIC );
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC, session );
				}
		);
		assertOptimisticLockIsApplied( scope );
	}


	@Test
	public void testFindApplyLockModeOptimistic2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.OPTIMISTIC );
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC, session );

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertOptimisticLockIsApplied( scope );
	}

	private void assertOptimisticLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsSelect( 0 );
		assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
		assertVersionIsSelected( statementInspector, 1 );
	}

	@Test
	public void testFindApplyLockModeForceIncrement(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.OPTIMISTIC_FORCE_INCREMENT
					);
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT, session);
				}
		);
		assertForceIncrementLockIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModeForceIncrement2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.OPTIMISTIC_FORCE_INCREMENT
					);
					assertEntityLockMode( testEntity, LockModeType.OPTIMISTIC_FORCE_INCREMENT, session);

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertForceIncrementLockIsApplied( scope );
	}

	private void assertForceIncrementLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsSelect( 0 );
		assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );

		assertVersionIsUpdated( statementInspector, 1 );
	}

	@Test
	public void testFindApplyLockModePessimisticRead(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_READ );
					assertEntityLockMode( testEntity, LockModeType.PESSIMISTIC_READ, session);
				}
		);
		assertPessimisticReadLockIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModePessimisticRead2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_READ );
					assertEntityLockMode( testEntity, LockModeType.PESSIMISTIC_READ, session);

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertPessimisticReadLockIsApplied( scope );
	}

	private void assertPessimisticReadLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );

		statementInspector.assertIsSelect( 0 );
		assertThat( sqlQueries.get( 0 ) ).contains( getDialect( scope ).getReadLockString( -1 ) );
	}

	@Test
	public void testFindApplyLockModePessimisticWrite(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_WRITE );
					assertEntityLockMode( testEntity, LockModeType.PESSIMISTIC_WRITE, session);
				}
		);
		assertPessimisticWriteLockIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModePessimisticWrite2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_WRITE );
					assertEntityLockMode( testEntity, LockModeType.PESSIMISTIC_WRITE, session);

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertPessimisticWriteLockIsApplied( scope );
	}

	private void assertPessimisticWriteLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );

		statementInspector.assertIsSelect( 0 );
		assertThat( sqlQueries.get( 0 ) ).contains( getDialect( scope ).getWriteLockString( -1 ) );
	}

	@Test
	public void testFindApplyLockModePessimisticForceIncrement(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.PESSIMISTIC_FORCE_INCREMENT
					);
					assertEntityLockMode( testEntity, LockModeType.PESSIMISTIC_FORCE_INCREMENT, session);
				}
		);
		assertPessimisticForceIncrementLockIsApplied( scope );
	}

	@Test
	public void testFindApplyLockModePessimisticForceIncrement2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertIsNotInitialized( session, proxy );

					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.PESSIMISTIC_FORCE_INCREMENT
					);
					assertEntityLockMode( testEntity, LockModeType.PESSIMISTIC_FORCE_INCREMENT, session);

					// batching is disabled because LockMode !=NONE, so the proxy has not been initialized
					assertIsNotInitialized( session, proxy );
				}
		);
		assertPessimisticForceIncrementLockIsApplied( scope );
	}

	private void assertPessimisticForceIncrementLockIsApplied(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsSelect( 0 );
		assertThat( sqlQueries.get( 0 ) ).contains( "for update" );

		assertVersionIsUpdated( statementInspector, 1 );
	}

	private static void assertIsNotInitialized(SessionImplementor session, TestEntity proxy) {
		assertFalse( Hibernate.isInitialized( proxy ) );
		assertEntityLockMode( proxy, LockModeType.NONE, session );
	}

	public void assertDoesNotContainLocks(String query, SessionFactoryScope scope) {
		Dialect dialect = getDialect( scope );
		String writeLockString = dialect.getWriteLockString( -1 );
		String readLockString = dialect.getReadLockString( -1 );
		String forUpdateString = dialect.getForUpdateString();
		assertThat( query ).doesNotContain( writeLockString );
		assertThat( query ).doesNotContain( readLockString );
		assertThat( query ).doesNotContain( forUpdateString );
	}

	private static void assertVersionIsUpdated(SQLStatementInspector statementInspector, int queryNumber) {
		statementInspector.assertIsUpdate( queryNumber );
		assertThat( statementInspector.getSqlQueries().get( queryNumber ) ).contains( "set version" );
	}

	private static void assertVersionIsSelected(SQLStatementInspector statementInspector, int queryNumber) {
		statementInspector.assertIsSelect( queryNumber );
		assertThat( statementInspector.getSqlQueries().get( queryNumber ) ).contains( "select version" );
	}

	private static Dialect getDialect(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getDialect();
	}

	private static void assertEntityLockMode(
			TestEntity testEntity,
			LockModeType optimistic,
			SessionImplementor session) {
		assertEquals(
				optimistic,
				session.getLockMode( testEntity ),
				"lock mode should be  " + optimistic.name()
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		@Version
		private Integer version;

		private String name;


		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public Integer getVersion() {
			return version;
		}

		public String getName() {
			return name;
		}

	}
}
