/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {OrderColumnListIndexBaseTest.Person.class, OrderColumnListIndexBaseTest.Phone.class} )
public class OrderColumnListIndexBaseTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::collections-customizing-ordered-list-ordinal-persist-example[]
			Person person = new Person(1L);
			entityManager.persist(person);
			person.addPhone(new Phone(1L, "landline", "028-234-9876"));
			person.addPhone(new Phone(2L, "mobile", "072-122-9876"));
			//end::collections-customizing-ordered-list-ordinal-persist-example[]
		});
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).inSession(
				session -> {
				session.doWork( conn -> {
					try (Statement st = conn.createStatement()) {
						st.execute( "select id, order_id from Phone" );
						ResultSet rs = st.getResultSet();
						while ( rs.next() ) {
							final long id = rs.getLong( 1 );
							if ( id == 1 ) {
								assertEquals( 100, rs.getInt( 2 ) );
							}
							else if ( id == 2 ) {
								assertEquals( 101, rs.getInt( 2 ) );
							}
						}
					}
				} );
			} );

		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(  Person.class, 1L);
			person.addPhone(new Phone(3L, "fax", "099-234-9876"));
			entityManager.persist(person);
		});

		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).inSession(
				session -> {
					session.doWork( conn -> {
						try (Statement st = conn.createStatement()) {
							st.execute( "select id, order_id from Phone" );
							try (ResultSet rs = st.getResultSet()) {
								while ( rs.next() ) {
									final long id = rs.getLong( 1 );
									if ( id == 1 ) {
										assertEquals( 100, rs.getInt( 2 ) );
									}
									else if ( id == 2 ) {
										assertEquals( 101, rs.getInt( 2 ) );
									}
									else if ( id == 3 ) {
										assertEquals( 102, rs.getInt( 2 ) );
									}
								}
							}
						}
					} );
				} );

		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(  Person.class, 1L);
			final List<Phone> phones = person.getPhones();
			assertEquals( Long.valueOf( 1L ), phones.get( 0 ).getId() );
			assertEquals( Long.valueOf( 2L ), phones.get( 1 ).getId() );
			assertEquals( Long.valueOf( 3L ), phones.get( 2 ).getId() );
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		//tag::collections-customizing-ordered-list-ordinal-mapping-example[]
		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		@ListIndexBase(100)
		private List<Phone> phones = new ArrayList<>();
		//end::collections-customizing-ordered-list-ordinal-mapping-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add(phone);
			phone.setPerson(this);
		}

		public void removePhone(Phone phone) {
			phones.remove(phone);
			phone.setPerson(null);
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		private String type;

		@Column(name = "`number`", unique = true)
		@NaturalId
		private String number;

		@ManyToOne
		private Person person;

		public Phone() {
		}

		public Phone(Long id, String type, String number) {
			this.id = id;
			this.type = type;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Phone phone = (Phone) o;
			return Objects.equals(number, phone.number);
		}

		@Override
		public int hashCode() {
			return Objects.hash(number);
		}
	}
}
