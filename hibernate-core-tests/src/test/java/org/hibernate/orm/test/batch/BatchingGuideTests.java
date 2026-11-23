package org.hibernate.orm.test.batch;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable batching examples with JDBC batch-call and persisted-state assertions.
///
/// @author Steve Ebersole
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = BatchingGuideTests.Person.class)
@SessionFactory
public class BatchingGuideTests implements ServiceRegistryProducer {
	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();
	private final Method addBatch;
	private final Method executeBatch;

	public BatchingGuideTests() throws Exception {
		addBatch = PreparedStatement.class.getMethod( "addBatch" );
		executeBatch = PreparedStatement.class.getMethod( "executeBatch" );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		builder.applySetting( BatchSettings.STATEMENT_BATCH_SIZE, 2 );
		builder.applySetting( JdbcSettings.DIALECT_NATIVE_PARAM_MARKERS, false );
		Object delegate = builder.getSettings().get( JdbcSettings.CONNECTION_PROVIDER );
		if ( delegate instanceof ConnectionProvider provider ) {
			connectionProvider.setConnectionProvider( provider );
		}
		return builder.applySetting( JdbcSettings.CONNECTION_PROVIDER, connectionProvider ).build();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		connectionProvider.clear();
	}

	@Test
	void insertsFlushAndClearWithPartialFinalBatch(SessionFactoryScope scope) {
		connectionProvider.clear();
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::batch-guide-inserts[]
			int flushInterval = 2;
			for ( long id = 1; id <= 5; id++ ) {
				entityManager.persist( new Person( id, "Person " + id ) );
				if ( id % flushInterval == 0 ) {
					entityManager.flush();
					entityManager.clear();
				}
			}
			entityManager.flush(); // write the final, incomplete group
			entityManager.clear();
			//end::batch-guide-inserts[]
		} );
		assertBatchCalls( "insert into batch_guide_person", 5, 3 );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuidePerson order by id", Person.class ).getResultList() )
				.hasSize( 5 ).allSatisfy( person -> assertThat( person.name ).isEqualTo( "Person " + person.id ) ) );
	}

	@Test
	void sessionOverrideAndRestoration(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::batch-guide-override[]
			Integer previousBatchSize = session.getJdbcBatchSize();
			try {
				session.setJdbcBatchSize( 5 );
				for ( long id = 1; id <= 5; id++ ) {
					session.persist( new Person( id, "Person " + id ) );
				}
				session.flush(); // execute this work before restoring the override
			}
			finally {
				session.setJdbcBatchSize( previousBatchSize );
			}
			//end::batch-guide-override[]
			assertThat( session.getJdbcBatchSize() ).isNull();
			assertBatchCalls( "insert into batch_guide_person", 5, 1 );
			connectionProvider.clear();
			for ( long id = 6; id <= 10; id++ ) {
				session.persist( new Person( id, "Person " + id ) );
			}
		} );
		assertBatchCalls( "insert into batch_guide_person", 5, 3 );
	}

	@Test
	void scrollingChangesDataAndBatchesUpdates(SessionFactoryScope scope) {
		preparePeople( scope );
		scope.inTransaction( session -> {
			//tag::batch-guide-scroll[]
			int flushInterval = 2;
			try ( var results = session.createQuery( "from BatchGuidePerson order by id", Person.class )
					.setCacheMode( CacheMode.IGNORE )
					.setFetchSize( 20 )
					.scroll( ScrollMode.FORWARD_ONLY ) ) {
				int processed = 0;
				while ( results.next() ) {
					Person person = results.get();
					person.name = "Processed " + person.id;
					if ( ++processed % flushInterval == 0 ) {
						session.flush();
						session.clear();
						//end::batch-guide-scroll[]
						assertThat( session.contains( person ) ).isFalse();
						//tag::batch-guide-scroll[]
					}
				}
				session.flush();
				session.clear();
			}
			//end::batch-guide-scroll[]
		} );
		assertBatchCalls( "update batch_guide_person", 5, 3 );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuidePerson order by id", Person.class ).getResultList() )
				.hasSize( 5 ).allSatisfy( person -> {
					assertThat( person.name ).isEqualTo( "Processed " + person.id );
					assertThat( person.version ).isEqualTo( 1 );
				} ) );
	}

	@Test
	void standardStatelessGroups(SessionFactoryScope scope) {
		var entityManagerFactory = scope.getSessionFactory();
		//tag::batch-guide-stateless[]
		List<Person> people = List.of( new Person( 1L, "First" ), new Person( 2L, "Second" ) );
		try ( var agent = entityManagerFactory.createEntityAgent() ) {
			var transaction = agent.getTransaction();
			transaction.begin();
			try {
				agent.insertMultiple( people );
				for ( Person person : people ) {
					person.name = "Processed " + person.id;
				}
				agent.updateMultiple( people );
				transaction.commit();
			}
			catch ( RuntimeException exception ) {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				throw exception;
			}
		}
		//end::batch-guide-stateless[]
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuidePerson order by id", Person.class ).getResultList() )
				.hasSize( 2 ).allSatisfy( person -> {
					assertThat( person.name ).isEqualTo( "Processed " + person.id );
					assertThat( person.version ).isEqualTo( 1 );
				} ) );
		//tag::batch-guide-stateless-delete[]
		try ( var agent = entityManagerFactory.createEntityAgent() ) {
			var transaction = agent.getTransaction();
			transaction.begin();
			try {
				agent.deleteMultiple( people );
				transaction.commit();
			}
			catch ( RuntimeException exception ) {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				throw exception;
			}
		}
		//end::batch-guide-stateless-delete[]
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuidePerson", Person.class ).getResultList() ).isEmpty() );
	}

	@Test
	void statelessFactorySettingAndExplicitOverride(SessionFactoryScope scope) {
		connectionProvider.clear();
		scope.inStatelessTransaction( session -> {
			assertThat( session.getJdbcBatchSize() ).isEqualTo( 0 );
			session.insert( new Person( 1L, "First" ) );
			session.insert( new Person( 2L, "Second" ) );
		} );
		assertBatchCalls( "insert into batch_guide_person", 0, 0 );
		connectionProvider.clear();
		scope.inStatelessTransaction( session -> {
			//tag::batch-guide-stateless-jdbc[]
			session.setJdbcBatchSize( 2 );
			session.insert( new Person( 3L, "Third" ) );
			session.insert( new Person( 4L, "Fourth" ) );
			session.insert( new Person( 5L, "Fifth" ) );
			//end::batch-guide-stateless-jdbc[]
		} );
		assertBatchCalls( "insert into batch_guide_person", 3, 2 );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuidePerson", Person.class ).getResultList() ).hasSize( 5 ) );
	}

	@Test
	void bulkMutationLeavesManagedStateStaleAndVersionsUnchanged(SessionFactoryScope scope) {
		preparePeople( scope );
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			Person managed = entityManager.find( Person.class, 1L );
			//tag::batch-guide-bulk-update[]
			entityManager.flush();
			int updated = entityManager.createStatement(
					"update BatchGuidePerson set name = :name where id = :id" )
					.setParameter( "name", "Bulk updated" ).setParameter( "id", 1L )
					.execute();
			entityManager.clear();
			//end::batch-guide-bulk-update[]
			assertThat( updated ).isEqualTo( 1 );
			assertThat( managed.name ).isEqualTo( "Person 1" );
			Person reloaded = entityManager.find( Person.class, 1L );
			assertThat( reloaded.name ).isEqualTo( "Bulk updated" );
			assertThat( reloaded.version ).isZero();
			//tag::batch-guide-versioned[]
			int versioned = entityManager.createStatement(
					"update versioned BatchGuidePerson set name = :name where id = :id" )
					.setParameter( "name", "Versioned update" ).setParameter( "id", 1L )
					.execute();
			entityManager.clear();
			//end::batch-guide-versioned[]
			assertThat( versioned ).isEqualTo( 1 );
			assertThat( entityManager.find( Person.class, 1L ).version ).isEqualTo( 1 );
		} );
	}

	@Test
	void bulkInsertAndDelete(SessionFactoryScope scope) {
		preparePeople( scope );
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::batch-guide-bulk-insert[]
			int inserted = entityManager.createStatement(
					"insert into BatchGuidePerson (id, name) select p.id + 10, p.name from BatchGuidePerson p" )
					.execute();
			//end::batch-guide-bulk-insert[]
			assertThat( inserted ).isEqualTo( 5 );
		} );
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::batch-guide-bulk-delete[]
			int deleted = entityManager.createStatement( "delete from BatchGuidePerson where id > :id" )
					.setParameter( "id", 10L ).execute();
			//end::batch-guide-bulk-delete[]
			assertThat( deleted ).isEqualTo( 5 );
		} );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuidePerson", Person.class ).getResultList() ).hasSize( 5 ) );
	}

	private void preparePeople(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 5; id++ ) {
				session.persist( new Person( id, "Person " + id ) );
			}
		} );
		connectionProvider.clear();
	}

	private void assertBatchCalls(String sqlPrefix, int additions, int executions) {
		var statements = connectionProvider.getPreparedStatementsAndSql().entrySet().stream()
				.filter( entry -> entry.getValue().startsWith( sqlPrefix ) ).map( java.util.Map.Entry::getKey ).toList();
		assertThat( statements ).isNotEmpty();
		assertThat( statements.stream().mapToInt( statement -> connectionProvider.spyContext
				.getCalls( addBatch, statement ).size() ).sum() ).isEqualTo( additions );
		assertThat( statements.stream().mapToInt( statement -> connectionProvider.spyContext
				.getCalls( executeBatch, statement ).size() ).sum() ).isEqualTo( executions );
	}

	//tag::batch-guide-model[]
	@Entity(name = "BatchGuidePerson")
	@Table(name = "batch_guide_person")
	public static class Person {
		@Id
		long id;
		String name;
		@Version
		int version;
	//end::batch-guide-model[]
		protected Person() {
		}
		Person(long id, String name) {
			this.id = id;
			this.name = name;
		}
	//tag::batch-guide-model[]
	}
	//end::batch-guide-model[]

}
