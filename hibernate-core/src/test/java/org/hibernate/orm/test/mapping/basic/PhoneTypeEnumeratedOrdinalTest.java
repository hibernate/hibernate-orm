/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.domain.userguide.PhoneType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {PhoneTypeEnumeratedOrdinalTest.Phone.class} )
public class PhoneTypeEnumeratedOrdinalTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::basic-enums-Enumerated-ordinal-persistence-example[]
			Phone phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-78990");
			phone.setType(PhoneType.MOBILE);
			entityManager.persist(phone);
			//end::basic-enums-Enumerated-ordinal-persistence-example[]
		});
	}

	//tag::basic-enums-Enumerated-ordinal-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "phone_number")
		private String number;

		@Enumerated(EnumType.ORDINAL)
		@Column(name = "phone_type")
		private PhoneType type;

		//Getters and setters are omitted for brevity

	//end::basic-enums-Enumerated-ordinal-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public PhoneType getType() {
			return type;
		}

		public void setType(PhoneType type) {
			this.type = type;
		}
	//tag::basic-enums-Enumerated-ordinal-example[]
	}
	//end::basic-enums-Enumerated-ordinal-example[]
}
