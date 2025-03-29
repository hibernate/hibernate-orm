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

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.domain.userguide.PhoneType;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class PhoneTypeEnumeratedStringTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Phone.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Phone phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-78990");
			phone.setType(PhoneType.MOBILE);
			entityManager.persist(phone);
		});
	}

	//tag::basic-enums-Enumerated-string-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "phone_number")
		private String number;

		@Enumerated(EnumType.STRING)
		@Column(name = "phone_type")
		private PhoneType type;

		//Getters and setters are omitted for brevity

	//end::basic-enums-Enumerated-string-example[]
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
	//tag::basic-enums-Enumerated-string-example[]
	}
	//end::basic-enums-Enumerated-string-example[]
}
