/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.distinct;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-10965")
@DomainModel(annotatedClasses = {SelectDistinctHqlTest.Person.class, SelectDistinctHqlTest.Phone.class})
@SessionFactory(useCollectingStatementInspector = true)
public class SelectDistinctHqlTest {

	private static final String DISTINCT_NAMED_QUERY = "distinct";
	private SQLStatementInspector SQLStatementInspector;

	@BeforeEach
	protected void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist( person );

			person.addPhone( new Phone( "027-123-4567" ) );
			person.addPhone( new Phone( "028-234-9876" ) );
		} );

		SQLStatementInspector = scope.getCollectingStatementInspector();
		SQLStatementInspector.clear();
	}

	@AfterEach
	protected void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SQLStatementInspector.getSqlQueries().clear();
			List<Person> persons = session.createQuery( "select distinct p from Person p", Person.class )
					.getResultList();
			String sqlQuery = SQLStatementInspector.getSqlQueries().get(0);
			assertEquals( 1, persons.size() );
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );

		scope.inTransaction( session -> {
			List<Person> persons = session.createQuery( "select p from Person p left join fetch p.phones ", Person.class )
					.getResultList();
			// with Hibernate ORM 6 it is not necessary to use *distinct* to not duplicate the instances which own the association
			assertEquals( 1, persons.size() );
		} );

		scope.inTransaction( session -> {
			SQLStatementInspector.getSqlQueries().clear();
			List<Person> persons = session.createQuery( "select distinct p from Person p left join fetch p.phones ", Person.class )
					.getResultList();
			assertEquals( 1, persons.size() );
			String sqlQuery = SQLStatementInspector.getSqlQueries().get(0);
			assertTrue( sqlQuery.contains( " distinct " ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13780")
	public void testNamedQueryDistinctPassThroughTrueWhenNotSpecified(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SQLStatementInspector.getSqlQueries().clear();
			List<Person> persons =
					session.createNamedQuery( DISTINCT_NAMED_QUERY, Person.class )
							.setMaxResults( 5 )
							.getResultList();
			assertEquals( 1, persons.size() );
			String sqlQuery = SQLStatementInspector.getSqlQueries().get(0);
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
