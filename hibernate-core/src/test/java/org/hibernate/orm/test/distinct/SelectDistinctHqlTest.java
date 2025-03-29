/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.distinct;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.SessionFactoryBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-10965")
public class SelectDistinctHqlTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final String DISTINCT_NAMED_QUERY = "distinct";
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

	protected void prepareTest() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			person.addPhone( new Phone( "027-123-4567" ) );
			person.addPhone( new Phone( "028-234-9876" ) );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery( "select distinct p from Person p" )
					.getResultList();
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertEquals( 1, persons.size() );
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<Person> persons = session.createQuery( "select p from Person p left join fetch p.phones " )
					.getResultList();
			// with Hibernate ORM 6 it is not necessary to use *distinct* to not duplicate the instances which own the association
			assertEquals( 1, persons.size() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons = session.createQuery( "select distinct p from Person p left join fetch p.phones " )
					.getResultList();
			assertEquals( 1, persons.size() );
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13780")
	public void testNamedQueryDistinctPassThroughTrueWhenNotSpecified() {
		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.getSqlQueries().clear();
			List<Person> persons =
					session.createNamedQuery( DISTINCT_NAMED_QUERY, Person.class )
							.setMaxResults( 5 )
							.getResultList();
			assertEquals( 1, persons.size() );
			String sqlQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );
	}

	@Entity(name = "Person")
	@NamedQueries({
			@NamedQuery(name = DISTINCT_NAMED_QUERY, query = "select distinct p from Person p left join fetch p.phones")
	})
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
