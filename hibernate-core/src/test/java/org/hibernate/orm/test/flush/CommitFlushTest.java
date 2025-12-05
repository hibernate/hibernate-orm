/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = {
		CommitFlushTest.Person.class,
		CommitFlushTest.Phone.class,
		CommitFlushTest.Advertisement.class,
})
public class CommitFlushTest {
	private final Logger log = Logger.getLogger( CommitFlushTest.class );

	@AfterEach
	public void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testFlushJPQL(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushJPQL");
			//tag::flushing-commit-flush-jpql-example[]
			Person person = new Person("John Doe");
			entityManager.persist(person);

			entityManager.createQuery("select p from Advertisement p")
				.setFlushMode(FlushModeType.COMMIT)
				.getResultList();

			entityManager.createQuery("select p from Person p")
				.setFlushMode(FlushModeType.COMMIT)
				.getResultList();
			//end::flushing-commit-flush-jpql-example[]
		});
	}

	@Test
	public void testFlushSQL(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushSQL");
			//tag::flushing-commit-flush-sql-example[]
			Person person = new Person("John Doe");
			entityManager.persist(person);

			assertEquals( 1, ((Number) entityManager
					.createNativeQuery( "select count(*) from Person" )
					.getSingleResult()).intValue() );
			//end::flushing-commit-flush-sql-example[]
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
