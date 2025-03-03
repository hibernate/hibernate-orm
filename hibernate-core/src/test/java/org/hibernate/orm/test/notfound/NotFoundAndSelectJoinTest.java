/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NotFoundAndSelectJoinTest.Person.class,
				NotFoundAndSelectJoinTest.Address.class,
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-15990")
public class NotFoundAndSelectJoinTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = new Address( 1l, "Texas", "Austin" );
					Person person = new Person( 2l, "And", address );

					session.persist( address );
					session.persist( person );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					List<Address> addresses = session.createQuery(
									"select a from Address a where  a.state = :state",
									Address.class
							)
							.setParameter( "state", "Texas" ).list();

					assertThat( addresses.size() ).isEqualTo( 1 );
					Address address = addresses.get( 0 );
					Person person = address.getPerson();
					assertThat( person ).isNotNull();
					assertThat( Hibernate.isInitialized( person ) );

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testQueryWithJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					List<Tuple> tuples = session.createQuery(
									"select a, p from Address a left join a.person p where a.state = :state",
									Tuple.class
							)
							.setParameter( "state", "Texas" ).list();

					assertThat( tuples.size() ).isEqualTo( 1 );
					Tuple tuple = tuples.get( 0 );
					Address address = tuple.get( 0, Address.class );

					Person person = address.getPerson();
					assertThat( person ).isNotNull();
					assertThat( Hibernate.isInitialized( person ) );

					Person p = tuple.get( 1, Person.class );
					assertThat( p ).isEqualTo( person );
					assertThat( p.getName() ).isEqualTo( "And" );

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
					assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 1 );

				}
		);
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		private Long id;

		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Person person;

		private String state;
		private String city;

		public Address() {
		}

		public Address(Long id, String state, String city) {
			this.id = id;
			this.state = state;
			this.city = city;
		}

		public Long getId() {
			return id;
		}

		public Person getPerson() {
			return person;
		}

		public String getState() {
			return state;
		}

		public String getCity() {
			return city;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(Long id, String name, Address address) {
			this.id = id;
			this.name = name;
			address.person = this;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}
}
