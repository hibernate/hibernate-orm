/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.sql.ResultSet;
import java.sql.SQLException;
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

import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OrderColumnListIndexBaseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::collections-customizing-ordered-list-ordinal-persist-example[]
			Person person = new Person(1L);
			entityManager.persist(person);
			person.addPhone(new Phone(1L, "landline", "028-234-9876"));
			person.addPhone(new Phone(2L, "mobile", "072-122-9876"));
			//end::collections-customizing-ordered-list-ordinal-persist-example[]
		});
		doInAutoCommit( st -> {
			try (ResultSet rs = st.executeQuery( "select id, order_id from Phone" )) {
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
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find(  Person.class, 1L);
			person.addPhone(new Phone(3L, "fax", "099-234-9876"));
			entityManager.persist(person);
		});
		doInAutoCommit( st -> {
			try (ResultSet rs = st.executeQuery( "select id, order_id from Phone" )) {
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
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );
		doInJPA(this::entityManagerFactory, entityManager -> {
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
