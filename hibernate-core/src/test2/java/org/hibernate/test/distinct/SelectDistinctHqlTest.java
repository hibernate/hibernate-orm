/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.distinct;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.jpa.QueryHints;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-10965" )
public class SelectDistinctHqlTest extends BaseNonConfigCoreFunctionalTestCase {

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
				"select distinct p from Person p" )
			.getResultList();
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
				"select distinct p from Person p" )
			.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, false )
			.getResultList();
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertFalse( sqlQuery.contains( " distinct " ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<Person> persons = session.createQuery(
				"select p from Person p left join fetch p.phones " )
			.getResultList();
			assertEquals(2, persons.size());
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
				"select distinct p from Person p left join fetch p.phones " )
			.getResultList();
			assertEquals(1, persons.size());
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery(
				"select distinct p from Person p left join fetch p.phones " )
			.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, false )
			.getResultList();
			assertEquals(1, persons.size());
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertFalse( sqlQuery.contains( " distinct " ) );
		} );
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
