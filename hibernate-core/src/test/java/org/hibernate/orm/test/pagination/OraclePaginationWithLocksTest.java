package org.hibernate.orm.test.pagination;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@RequiresDialect(value = OracleDialect.class, majorVersion = 12)
@TestForIssue(jiraKey = "HHH-14624")
@DomainModel(
		annotatedClasses = OraclePaginationWithLocksTest.Person.class
)
@SessionFactory(
		statementInspectorClass = OraclePaginationWithLocksTest.MostRecentStatementInspector.class
)
public class OraclePaginationWithLocksTest {
	private MostRecentStatementInspector mostRecentStatementInspector;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 20; i++ ) {
						session.persist( new Person( "name" + i ) );
					}
					session.persist( new Person( "for update" ) );
				}
		);
		mostRecentStatementInspector = (MostRecentStatementInspector) scope.getStatementInspector();
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Person" ).executeUpdate()

		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<Person> people = session.createNativeQuery( "select * from Person for update" )
							.setMaxResults( 10 )
							.list();
					assertEquals( 10, people.size() );
					assertFalse( mostRecentStatementInspector.sqlContains( "fetch" ) );
					assertTrue( mostRecentStatementInspector.sqlContains( "rownum" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final List<Person> people = session.createNativeQuery( "select * from Person" )
							.setMaxResults( 10 )
							.list();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					final List<Person> people = session.createNativeQuery( "select * from Person" )
							.setFirstResult( 3 )
							.setMaxResults( 10 )
							.list();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);
	}

	@Test
	public void testNativeQueryWithSpaces(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Person> people = session.createNativeQuery(
					"select  p.name from Person p where p.id = 1 for update" )
					.setMaxResults( 10 )
					.list();
		} );

		scope.inTransaction( session -> {
			Person p = new Person();
			p.setName( " this is a  string with spaces  " );
			session.persist( p );
		} );

		scope.inTransaction( session -> {
			final List<Person> people = session.createNativeQuery(
					"select p.name from Person p where p.name =  ' this is a  string with spaces  ' for update" )
					.setMaxResults( 10 )
					.list();
			assertEquals( 1, people.size() );
		} );
	}

	@Test
	public void testCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final CriteriaQuery<Person> query = session.getCriteriaBuilder().createQuery( Person.class );
					final Root<Person> root = query.from( Person.class );
					query.select( root );
					final List<Person> people = session.createQuery( query )
							.setMaxResults( 10 )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
							.getResultList();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					final CriteriaQuery<Person> query = session.getCriteriaBuilder().createQuery( Person.class );
					final Root<Person> root = query.from( Person.class );
					query.select( root );
					final List<Person> people = session.createQuery( query )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					final CriteriaQuery<Person> query = session.getCriteriaBuilder().createQuery( Person.class );
					final Root<Person> root = query.from( Person.class );
					query.select( root );
					final List<Person> people = session.createQuery( query )
							.setMaxResults( 10 )
							.setFirstResult( 2 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

	}

	@Test
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p", Person.class )
							.setMaxResults( 10 )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
							.getResultList();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p", Person.class )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p", Person.class )
							.setFirstResult( 2 )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertEquals( 10, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p where p.name = 'for update'", Person.class )
							.setMaxResults( 10 )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
							.getResultList();
					assertEquals( 1, people.size() );
					assertSqlContainsFetch( session );
				}
		);

		scope.inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p where p.name = 'for update'", Person.class )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 1, people.size() );
					assertSqlContainsFetch( session );
				}
		);


	}

	private void assertSqlContainsFetch(SessionImplementor session) {
		// We can only assert for fetch if the database actually supports it
		if ( session.getFactory().getJdbcServices().getDialect().supportsFetchClause( FetchClauseType.ROWS_ONLY ) ) {
			assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
		}
		else {
			assertTrue( mostRecentStatementInspector.sqlContains( "rownum" ) );
		}
	}

	public static class MostRecentStatementInspector implements StatementInspector {
		private String mostRecentSql;

		public String inspect(String sql) {
			mostRecentSql = sql;
			return sql;
		}

		public boolean sqlContains(String toCheck) {
			return mostRecentSql.contains( toCheck );
		}

	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
