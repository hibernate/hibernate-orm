/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneBidirectionalTest extends BaseEntityManagerFunctionalTestCase {

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

			phone.addDetails(details);
			entityManager.persist(phone);
		});
	}

	@Test
	public void testConstraint() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::associations-one-to-one-bidirectional-lifecycle-example[]
			Phone phone = new Phone("123-456-7890");
			PhoneDetails details = new PhoneDetails("T-Mobile", "GSM");

			phone.addDetails(details);
			entityManager.persist(phone);
			//end::associations-one-to-one-bidirectional-lifecycle-example[]
		});
		try {
			doInJPA(this::entityManagerFactory, entityManager -> {

				Phone phone = entityManager.find(Phone.class, 1L);

				//tag::associations-one-to-one-bidirectional-constraint-example[]
				PhoneDetails otherDetails = new PhoneDetails("T-Mobile", "CDMA");
				otherDetails.setPhone(phone);
				entityManager.persist(otherDetails);
				entityManager.flush();
				entityManager.clear();

				//throws jakarta.persistence.PersistenceException: org.hibernate.HibernateException: More than one row with the given identifier was found: 1
				phone = entityManager.find(Phone.class, phone.getId());
				//end::associations-one-to-one-bidirectional-constraint-example[]
				phone.getDetails().getProvider();
			});
			Assert.fail("Expected: HHH000327: Error performing load command : org.hibernate.HibernateException: More than one row with the given identifier was found: 1");
		}
		catch (Exception expected) {
			log.error("Expected", expected);
		}
	}

	//tag::associations-one-to-one-bidirectional-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`number`")
		private String number;

		@OneToOne(
			mappedBy = "phone",
			cascade = CascadeType.ALL,
			orphanRemoval = true,
			fetch = FetchType.LAZY
		)
		private PhoneDetails details;

		//Getters and setters are omitted for brevity

	//end::associations-one-to-one-bidirectional-example[]

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

	//tag::associations-one-to-one-bidirectional-example[]
		public void addDetails(PhoneDetails details) {
			details.setPhone(this);
			this.details = details;
		}

		public void removeDetails() {
			if (details != null) {
				details.setPhone(null);
				this.details = null;
			}
		}
	}

	@Entity(name = "PhoneDetails")
	public static class PhoneDetails {

		@Id
		@GeneratedValue
		private Long id;

		private String provider;

		private String technology;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "phone_id")
		private Phone phone;

		//Getters and setters are omitted for brevity

	//end::associations-one-to-one-bidirectional-example[]

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

		public Phone getPhone() {
			return phone;
		}

		public void setPhone(Phone phone) {
			this.phone = phone;
		}
	//tag::associations-one-to-one-bidirectional-example[]
	}
	//end::associations-one-to-one-bidirectional-example[]
}
