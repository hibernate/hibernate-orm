package org.hibernate.orm.test.jpa.lock;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
@RequiresDialect( H2Dialect.class )
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
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from TestEntity" ).executeUpdate()
		);
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					TestEntity testEntity = session.find( TestEntity.class, 1 );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModeNone(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.NONE );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModeNone2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.NONE );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModeRead(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.READ );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsSelected( statementInspector );
	}

	@Test
	public void testFindApplyLockModeRead2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.READ );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks(  sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsSelected( statementInspector );
	}

	@Test
	public void testFindApplyLockModeWrite(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.WRITE );
					assertEquals(
							"lock mode should be  OPTIMISTIC_FORCE_INCREMENT",
							LockModeType.OPTIMISTIC_FORCE_INCREMENT,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsUpdated( statementInspector );
	}

	@Test
	public void testFindApplyLockModeWrite2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					/*
					 	two ids to load, we are generating an additional query to upgrade the lock only to the
					 	entity with id 1
					 */
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.WRITE );
					assertEquals(
							"lock mode should be  OPTIMISTIC_FORCE_INCREMENT",
							LockModeType.OPTIMISTIC_FORCE_INCREMENT,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsUpdated( statementInspector );
	}

	@Test
	public void testFindApplyLockModeOptimistic(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.OPTIMISTIC );
					assertEquals(
							"lock mode should be  OPTIMISTIC",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsSelected( statementInspector );
	}



	@Test
	public void testFindApplyLockModeOptimistic2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					/*
					 	two ids to load, we are generating an additional query to upgrade the lock only to the
					 	entity with id 1
					 */
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.OPTIMISTIC );
					assertEquals(
							"lock mode should be  OPTIMISTIC",
							LockModeType.OPTIMISTIC,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsSelected( statementInspector );
	}

	@Test
	public void testFindApplyLockModeForceIncrement(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.OPTIMISTIC_FORCE_INCREMENT
					);
					assertEquals(
							"lock mode should be OPTIMISTIC_FORCE_INCREMENT ",
							LockModeType.OPTIMISTIC_FORCE_INCREMENT,
							session.getLockMode( testEntity )
					);
					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsUpdated( statementInspector );
	}

	@Test
	public void testFindApplyLockModeForceIncrement2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					/*
					 	two ids to load, we are generating an additional query to upgrade the lock only to the
					 	entity with id 1
					 */
					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.OPTIMISTIC_FORCE_INCREMENT
					);
					assertEquals(
							"lock mode should be OPTIMISTIC_FORCE_INCREMENT ",
							LockModeType.OPTIMISTIC_FORCE_INCREMENT,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );

					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					statementInspector.clear();
				}
		);
		assertVersionIsUpdated( statementInspector );
	}

	@Test
	public void testFindApplyLockModePessimisticRead(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_READ );
					assertEquals(
							"lock mode should be PESSIMISTIC_READ ",
							LockModeType.PESSIMISTIC_READ,
							session.getLockMode( testEntity )
					);
					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertThat( sqlQueries.get( 0 ) ).contains( getDialect( scope ).getReadLockString( -1 ) );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModePessimisticRead2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					/*
					 	two ids to load, we are generating an additional query to upgrade the lock only to the
					 	entity with id 1
					 */
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_READ );
					assertEquals(
							"lock mode should be PESSIMISTIC_READ ",
							LockModeType.PESSIMISTIC_READ,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 2 );

					String readLockString = getDialect( scope ).getReadLockString( -1 );
					assertThat( sqlQueries.get( 0 ) ).doesNotContain( readLockString );
					assertThat( sqlQueries.get( 1 ) ).contains( readLockString );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModePessimisticWrite(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_WRITE );
					assertEquals(
							"lock mode should be PESSIMISTIC_WRITE ",
							LockModeType.PESSIMISTIC_WRITE,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 1 );
					assertThat( sqlQueries.get( 0 ) ).contains( getDialect( scope ).getWriteLockString( -1 ) );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModePessimisticWrite2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					/*
					 	two ids to load, we are generating an additional query to upgrade the lock only to the
					 	entity with id 1
					 */
					TestEntity testEntity = session.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_WRITE );
					assertEquals(
							"lock mode should be PESSIMISTIC_WRITE ",
							LockModeType.PESSIMISTIC_WRITE,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 2 );


					String readLockString = getDialect( scope ).getWriteLockString( -1 );
					assertThat( sqlQueries.get( 0 ) ).doesNotContain( readLockString );
					assertThat( sqlQueries.get( 1 ) ).contains( readLockString );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModePessimisticForceIncrement(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// only one id to load
					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.PESSIMISTIC_FORCE_INCREMENT
					);
					assertEquals(
							"lock mode should be PESSIMISTIC_FORCE_INCREMENT ",
							LockModeType.PESSIMISTIC_FORCE_INCREMENT,
							session.getLockMode( testEntity )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 2 );
					assertThat( sqlQueries.get( 0 ) ).contains( "for update" );
					statementInspector.assertIsUpdate( 1 );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
	}

	@Test
	public void testFindApplyLockModePessimisticForceIncrement2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity proxy = session.getReference( TestEntity.class, 2 );
					assertFalse( Hibernate.isInitialized( proxy ) );

					/*
					 	two ids to load, we are generating an additional query to upgrade the lock only to the
					 	entity with id 1
					 */
					TestEntity testEntity = session.find(
							TestEntity.class,
							1,
							LockModeType.PESSIMISTIC_FORCE_INCREMENT
					);
					assertEquals(
							"lock mode should be PESSIMISTIC_FORCE_INCREMENT ",
							LockModeType.PESSIMISTIC_FORCE_INCREMENT,
							session.getLockMode( testEntity )
					);

					assertTrue( Hibernate.isInitialized( proxy ) );
					assertEquals(
							"lock mode should be OPTIMISTIC ",
							LockModeType.OPTIMISTIC,
							session.getLockMode( proxy )
					);

					List<String> sqlQueries = statementInspector.getSqlQueries();
					assertThat( sqlQueries.size() ).isEqualTo( 3 );
					assertDoesNotContainLocks( sqlQueries.get( 0 ), scope );
					assertThat( sqlQueries.get( 1 ) ).contains( "for update" );
					statementInspector.assertIsUpdate( 2 );
					statementInspector.clear();
				}
		);
		assertNoOtherQueriesAreExecuted( statementInspector );
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

	private static void assertVersionIsUpdated(SQLStatementInspector statementInspector) {
		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );
		statementInspector.assertIsUpdate( 0 );
		assertThat(sqlQueries.get( 0 )).contains( "set version"  );
	}

	private static void assertVersionIsSelected(SQLStatementInspector statementInspector) {
		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 1 );
		assertThat( sqlQueries.get( 0 ) ).contains( "select version" );
	}

	private static void assertNoOtherQueriesAreExecuted(SQLStatementInspector statementInspector) {
		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 0 );
	}

	private static Dialect getDialect(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getDialect();
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
