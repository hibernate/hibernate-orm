/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		ManualFlushTest.Person.class,
		ManualFlushTest.Phone.class,
		ManualFlushTest.Advertisement.class,
})
@SessionFactory
public class ManualFlushTest {
	private final Logger log = Logger.getLogger( ManualFlushTest.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testFlushSQL(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			log.info("testFlushSQL");
			//tag::flushing-manual-flush-example[]
			Person person = new Person("John Doe");
			entityManager.persist(person);

			Session session = entityManager.unwrap(Session.class);
			session.setHibernateFlushMode(FlushMode.MANUAL);

			Assertions.assertEquals( 0, ((Number) entityManager
					.createQuery( "select count(id) from Person" )
					.getSingleResult()).intValue() );

			Assertions.assertEquals( 0, (int) session.createNativeQuery( "select count(*) from Person", Integer.class )
					.uniqueResult() );
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
