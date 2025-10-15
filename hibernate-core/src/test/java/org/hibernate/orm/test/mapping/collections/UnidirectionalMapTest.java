/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TemporalType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {UnidirectionalMapTest.Person.class, UnidirectionalMapTest.Phone.class} )
public class UnidirectionalMapTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person(1L);
			LocalDateTime now = LocalDateTime.now();
			person.addPhone(
				new Phone(PhoneType.LAND_LINE, "028-234-9876", Timestamp.valueOf(now))
			);
			person.addPhone(
				new Phone(PhoneType.MOBILE, "072-122-9876", Timestamp.valueOf(now.minusDays(1)))
			);
			entityManager.persist(person);
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			Map<Date, Phone> phones = person.getPhoneRegister();
			Assertions.assertEquals(2, phones.size());
		});
	}

	//tag::collections-map-unidirectional-example[]
	public enum PhoneType {
		LAND_LINE,
		MOBILE
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinTable(
			name = "phone_register",
			joinColumns = @JoinColumn(name = "phone_id"),
			inverseJoinColumns = @JoinColumn(name = "person_id"))
		@MapKey(name = "since")
		@MapKeyTemporal(TemporalType.TIMESTAMP)
		private Map<Date, Phone> phoneRegister = new HashMap<>();

		//Getters and setters are omitted for brevity

	//end::collections-map-unidirectional-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public Map<Date, Phone> getPhoneRegister() {
			return phoneRegister;
		}

	//tag::collections-map-unidirectional-example[]
		public void addPhone(Phone phone) {
			phoneRegister.put(phone.getSince(), phone);
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		private PhoneType type;

		@Column(name = "`number`")
		private String number;

		private Date since;

		//Getters and setters are omitted for brevity

	//end::collections-map-unidirectional-example[]

		public Phone() {
		}

		public Phone(PhoneType type, String number, Date since) {
			this.type = type;
			this.number = number;
			this.since = since;
		}

		public PhoneType getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}

		public Date getSince() {
			return since;
		}
	//tag::collections-map-unidirectional-example[]
	}
	//end::collections-map-unidirectional-example[]
}
