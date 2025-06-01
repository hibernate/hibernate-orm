/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Selaron
 */
public class OrderColumnListIndexHHH18771ListInitializerTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( 1L );
			entityManager.persist( person );
			person.addPhone( new Phone( 1L ) );
			person.getChildren().add( new Person( 2L ) );
			person.getChildren().get( 0 ).setMother( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			assertNotNull( person );
			assertEquals( 1, person.getPhones().size() );
			assertNotNull( person.getPhones().get( 0 ) );
			assertEquals( 1, person.getChildren().size() );
			assertEquals( person, person.getChildren().get( 0 ).getMother() );
		} );
	}

	@Entity(name = "Person")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Person {

		@Id
		private Long id;

		@OneToMany(fetch = FetchType.EAGER, mappedBy = "person", cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		@ListIndexBase(100)
		private List<Phone> phones = new ArrayList<>();

		@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinTable(name = "parent_child_relationships", joinColumns = @JoinColumn(name = "parent_id"), inverseJoinColumns = @JoinColumn(name = "child_id"))
		@OrderColumn(name = "pos")
		@ListIndexBase(200)
		private List<Person> children = new ArrayList<>();

		@ManyToOne
		@JoinColumn(name = "mother_id")
		private Person mother;

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}

		public List<Person> getChildren() {
			return children;
		}

		public Person getMother() {
			return mother;
		}

		public void setMother(Person mother) {
			this.mother = mother;
		}

		public void removePhone(Phone phone) {
			phones.remove( phone );
			phone.setPerson( null );
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@ManyToOne
		private Person person;

		public Phone() {
		}

		public Phone(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}
}
