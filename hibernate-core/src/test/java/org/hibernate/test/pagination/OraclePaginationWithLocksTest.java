package org.hibernate.test.pagination;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RequiresDialect(Oracle12cDialect.class)
@TestForIssue(jiraKey = "HHH-14624")
public class OraclePaginationWithLocksTest extends BaseCoreFunctionalTestCase {
	private static final MostRecentStatementInspector mostRecentStatementInspector = new MostRecentStatementInspector();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.STATEMENT_INSPECTOR, mostRecentStatementInspector );
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					for ( int i = 0; i < 20; i++ ) {
						session.persist( new Person( "name" + i ) );
					}
					session.persist( new Person( "for update" ) );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session ->
						session.createQuery( "delete from Person" ).executeUpdate()

		);
	}

	@Test
	public void testNativeQuery() {
		inTransaction(
				session -> {
					final List<Person> people = session.createNativeQuery( "select * from Person for update" )
							.setMaxResults( 10 )
							.list();
					assertEquals( 10, people.size() );
					assertFalse( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					final List<Person> people = session.createNativeQuery( "select * from Person" )
							.setMaxResults( 10 )
							.list();
					assertEquals( 10, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					final List<Person> people = session.createNativeQuery( "select * from Person" )
							.setFirstResult( 3 )
							.setMaxResults( 10 )
							.list();
					assertEquals( 10, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);
	}

	@Test
	public void testNativeQueryWithSpaces() {
		inTransaction( session -> {
			final List<Person> people = session.createNativeQuery(
					"select  p.name from Person p where p.id = 1 for update" )
					.setMaxResults( 10 )
					.list();
		} );

		inTransaction( session -> {
			Person p = new Person();
			p.setName( " this is a  string with spaces  " );
			session.persist( p );
		} );

		inTransaction( session -> {
			final List<Person> people = session.createNativeQuery(
					"select p.name from Person p where p.name =  ' this is a  string with spaces  ' for update" )
					.setMaxResults( 10 )
					.list();
			assertEquals( 1, people.size() );
		} );
	}

	@Test
	public void testCriteriaQuery() {
		inTransaction(
				session -> {
					final CriteriaQuery<Person> query = session.getCriteriaBuilder().createQuery( Person.class );
					final Root<Person> root = query.from( Person.class );
					query.select( root );
					final List<Person> people = session.createQuery( query )
							.setMaxResults( 10 )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
							.getResultList();
					assertEquals( 10, people.size() );
					assertFalse( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					final CriteriaQuery<Person> query = session.getCriteriaBuilder().createQuery( Person.class );
					final Root<Person> root = query.from( Person.class );
					query.select( root );
					final List<Person> people = session.createQuery( query )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					final CriteriaQuery<Person> query = session.getCriteriaBuilder().createQuery( Person.class );
					final Root<Person> root = query.from( Person.class );
					query.select( root );
					final List<Person> people = session.createQuery( query )
							.setMaxResults( 10 )
							.setFirstResult( 2 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

	}

	@Test
	public void testHqlQuery() {
		inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p", Person.class )
							.setMaxResults( 10 )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
							.getResultList();
					assertEquals( 10, people.size() );
					assertFalse( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p", Person.class )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p", Person.class )
							.setFirstResult( 2 )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 10, people.size() );
					assertEquals( 10, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p where p.name = 'for update'", Person.class )
							.setMaxResults( 10 )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
							.getResultList();
					assertEquals( 1, people.size() );
					assertFalse( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);

		inTransaction(
				session -> {
					List<Person> people = session.createQuery(
							"select p from Person p where p.name = 'for update'", Person.class )
							.setMaxResults( 10 )
							.getResultList();
					assertEquals( 1, people.size() );
					assertTrue( mostRecentStatementInspector.sqlContains( "fetch" ) );
				}
		);


	}

	private static class MostRecentStatementInspector implements StatementInspector {
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
