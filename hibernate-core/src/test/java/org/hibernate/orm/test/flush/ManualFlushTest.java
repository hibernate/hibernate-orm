/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ManualFlushTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Person.class,
				Phone.class,
				Advertisement.class,
		};
	}

	@Test
	public void testFlushSQL() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery("delete from Person").executeUpdate();
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			log.info("testFlushSQL");
			//tag::flushing-manual-flush-example[]
			Person person = new Person("John Doe");
			entityManager.persist(person);

			Session session = entityManager.unwrap(Session.class);
			session.setHibernateFlushMode(FlushMode.MANUAL);

			assertTrue(((Number) entityManager
				.createQuery("select count(id) from Person")
				.getSingleResult()).intValue() == 0);

			assertTrue( session.createNativeQuery( "select count(*) from Person", Integer.class )
					.uniqueResult() == 0);
			//end::flushing-manual-flush-example[]
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		private List<Phone> phones = new ArrayList<>();

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add(phone);
			phone.setPerson(this);
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Person person;

		@Column(name = "`number`")
		private String number;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
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
	}

	@Entity(name = "Advertisement")
	public static class Advertisement {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
