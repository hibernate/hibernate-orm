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
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				OneToOneBidirectionalLazyTest.Phone.class,
				OneToOneBidirectionalLazyTest.PhoneDetails.class,
		}
)
public class OneToOneBidirectionalLazyTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

		} );
	}

	//tag::associations-one-to-one-bidirectional-lazy-example[]
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

		//end::associations-one-to-one-bidirectional-lazy-example[]

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

		//tag::associations-one-to-one-bidirectional-lazy-example[]
		public void addDetails(PhoneDetails details) {
			details.setPhone( this );
			this.details = details;
		}

		public void removeDetails() {
			if ( details != null ) {
				details.setPhone( null );
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

		//end::associations-one-to-one-bidirectional-lazy-example[]

		public PhoneDetails() {
		}

		public PhoneDetails(String provider, String technology) {
			this.provider = provider;
			this.technology = technology;
		}
		//Getters and setters are omitted for brevity

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
		//tag::associations-one-to-one-bidirectional-lazy-example[]
	}
	//end::associations-one-to-one-bidirectional-lazy-example[]
}
