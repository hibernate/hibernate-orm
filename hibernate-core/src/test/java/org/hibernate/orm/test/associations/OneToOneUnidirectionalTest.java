/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneUnidirectionalTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Phone.class,
				PhoneDetails.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Phone phone = new Phone("123-456-7890");
			PhoneDetails details = new PhoneDetails("T-Mobile", "GSM");

			phone.setDetails(details);
			entityManager.persist(phone);
			entityManager.persist(details);
		});
	}

	//tag::associations-one-to-one-unidirectional-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`number`")
		private String number;

		@OneToOne
		@JoinColumn(name = "details_id")
		private PhoneDetails details;

		//Getters and setters are omitted for brevity

	//end::associations-one-to-one-unidirectional-example[]

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

		public PhoneDetails getDetails() {
			return details;
		}

		public void setDetails(PhoneDetails details) {
			this.details = details;
		}
	//tag::associations-one-to-one-unidirectional-example[]
	}

	@Entity(name = "PhoneDetails")
	public static class PhoneDetails {

		@Id
		@GeneratedValue
		private Long id;

		private String provider;

		private String technology;

		//Getters and setters are omitted for brevity

	//end::associations-one-to-one-unidirectional-example[]

		public PhoneDetails() {
		}

		public PhoneDetails(String provider, String technology) {
			this.provider = provider;
			this.technology = technology;
		}

		public String getProvider() {
			return provider;
		}

		public String getTechnology() {
			return technology;
		}

		public void setTechnology(String technology) {
			this.technology = technology;
		}
	//tag::associations-one-to-one-unidirectional-example[]
	}
	//end::associations-one-to-one-unidirectional-example[]
}
