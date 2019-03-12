/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.distinct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-13280" )
public class SelectDistinctCommentHqlTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.USE_SQL_COMMENTS, true );
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			person.addPhone( new Phone( "027-123-4567" ) );
			person.addPhone( new Phone( "028-234-9876" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
					"select distinct p from Person p left join fetch p.phones ", Person.class)
					.setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false)
					.setComment("PersonWithPhones")
					.setMaxResults(5)
					.getResultList();
			assertEquals(1, persons.size());
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertFalse( removeCommentFromQuery( sqlQuery ).contains( " distinct " ) );
			assertTrue( getCommentFromQuery( sqlQuery ).contains( "/* PersonWithPhones */" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person person = session.find( Person.class, 1L );
			session.remove( person );
		} );
	}

	@Test
	public void testDefaultComment() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			person.addPhone( new Phone( "027-123-4567" ) );
			person.addPhone( new Phone( "028-234-9876" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
					"select distinct p from Person p left join fetch p.phones ", Person.class)
					.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, false )
					.setMaxResults( 5 )
					.getResultList();
			assertEquals( 1, persons.size() );
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			// "distinct" should have been removed from the (non-comment) SQL.
			assertFalse( removeCommentFromQuery( sqlQuery ).contains( " distinct ") );
			// The default comment should be the HQL. Shouldn't it contain "distinct"???
			assertTrue( getCommentFromQuery( sqlQuery ).contains( " distinct " ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person person = session.find( Person.class, 1L );
			session.remove( person );
		} );
	}

	@Test
	public void testExplicitComment() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			person.addPhone( new Phone( "027-123-4567" ) );
			person.addPhone( new Phone( "028-234-9876" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
					"select distinct p from Person p left join fetch p.phones ", Person.class)
					.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, false )
					.setComment( "select distinct in comment" )
					.setMaxResults( 5 )
					.getResultList();
			assertEquals( 1, persons.size() );
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertFalse( removeCommentFromQuery( sqlQuery ).contains( " distinct " ) );
			// An explicit comment shouldn't be changed
			assertTrue( getCommentFromQuery( sqlQuery ).contains( "select distinct in comment" ) );
		} );

		doInHibernate(
			this::sessionFactory, session -> {
				Person person = session.find( Person.class, 1L );
				session.remove( person );
			}
		);
	}

	@Test
	public void testSelectDistinctInSubquery() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			person.addPhone( new Phone( "027-123-4567" ) );
			person.addPhone( new Phone( "028-234-9876" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
					"select p from Person p where p.id in " +
							"( select distinct ph.person.id from Phone ph where locate( '027', ph.number ) > 0 )",
					Person.class)
					.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, false )
					.setComment( "a comment" )
					.setMaxResults( 5 )
					.getResultList();
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			// "distinct" in a subquery shouldn't be removed
			assertTrue( removeCommentFromQuery( sqlQuery ).contains( "select distinct" ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person person = session.find( Person.class, 1L );
			session.remove( person );
		} );
	}

	final String removeCommentFromQuery(String sql) {
		final int positionEndComment = sql.lastIndexOf( "*/" );
		return sql.substring( positionEndComment + 2 );
	}

	final String getCommentFromQuery(String sql) {
		final int positionEndComment = sql.lastIndexOf( "*/" );
		return sql.substring( 0, positionEndComment + 2 );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Phone> phones = new ArrayList<>();

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.person = this;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@Column(name = "`number`")
		private String number;

		@ManyToOne
		private Person person;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}
	}
}
