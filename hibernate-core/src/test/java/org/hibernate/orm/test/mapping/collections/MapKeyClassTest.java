/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MapKeyClassTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::collections-map-key-class-persist-example[]
			Person person = new Person();
			person.setId(1L);
			person.getCallRegister().put(new MobilePhone("01", "234", "567"), 101);
			person.getCallRegister().put(new MobilePhone("01", "234", "789"), 102);

			entityManager.persist(person);
			//end::collections-map-key-class-persist-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::collections-map-key-class-fetch-example[]
			Person person = entityManager.find(Person.class, 1L);
			assertEquals(2, person.getCallRegister().size());

			assertEquals(
				Integer.valueOf(101),
				person.getCallRegister().get(MobilePhone.fromString("01-234-567"))
			);

			assertEquals(
				Integer.valueOf(102),
				person.getCallRegister().get(MobilePhone.fromString("01-234-789"))
			);
			//end::collections-map-key-class-fetch-example[]
		});
	}

	//tag::collections-map-key-class-mapping-example[]
	@Entity
	@Table(name = "person")
	public static class Person {

		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(
			name = "call_register",
			joinColumns = @JoinColumn(name = "person_id")
		)
		@MapKeyColumn(name = "call_timestamp_epoch")
		@MapKeyClass(MobilePhone.class)
		@Column(name = "call_register")
		private Map<PhoneNumber, Integer> callRegister = new HashMap<>();

		//Getters and setters are omitted for brevity
	//end::collections-map-key-class-mapping-example[]

		public void setId(Long id) {
			this.id = id;
		}

		public Map<PhoneNumber, Integer> getCallRegister() {
			return callRegister;
		}
	//tag::collections-map-key-class-mapping-example[]
	}
	//end::collections-map-key-class-mapping-example[]

	//tag::collections-map-key-class-type-mapping-example[]
	public interface PhoneNumber {

		String get();
	}

	@Embeddable
	public static class MobilePhone
			implements PhoneNumber {

		static PhoneNumber fromString(String phoneNumber) {
			String[] tokens = phoneNumber.split("-");
			if (tokens.length != 3) {
				throw new IllegalArgumentException("invalid phone number: " + phoneNumber);
			}
			int i = 0;
			return new MobilePhone(
				tokens[i++],
				tokens[i++],
				tokens[i]
			);
		}

		private MobilePhone() {
		}

		public MobilePhone(
				String countryCode,
				String operatorCode,
				String subscriberCode) {
			this.countryCode = countryCode;
			this.operatorCode = operatorCode;
			this.subscriberCode = subscriberCode;
		}

		@Column(name = "country_code")
		private String countryCode;

		@Column(name = "operator_code")
		private String operatorCode;

		@Column(name = "subscriber_code")
		private String subscriberCode;

		@Override
		public String get() {
			return String.format(
				"%s-%s-%s",
				countryCode,
				operatorCode,
				subscriberCode
			);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MobilePhone that = (MobilePhone) o;
			return Objects.equals(countryCode, that.countryCode) &&
					Objects.equals(operatorCode, that.operatorCode) &&
					Objects.equals(subscriberCode, that.subscriberCode);
		}

		@Override
		public int hashCode() {
			return Objects.hash(countryCode, operatorCode, subscriberCode);
		}
	}
	//end::collections-map-key-class-type-mapping-example[]
}
