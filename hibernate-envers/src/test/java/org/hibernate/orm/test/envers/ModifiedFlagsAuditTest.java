/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = ModifiedFlagsAuditTest.Customer.class)
public class ModifiedFlagsAuditTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = new Customer();
			customer.setId(1L);
			customer.setFirstName("John");
			customer.setLastName("Doe");

			entityManager.persist(customer);
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-tracking-properties-changes-example[]
			Customer customer = entityManager.find(Customer.class, 1L);
			customer.setLastName("Doe Jr.");
			//end::envers-tracking-properties-changes-example[]
		});
	}

	//tag::envers-tracking-properties-changes-mapping-example[]
	@Audited(withModifiedFlag = true)
	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "created_on")
		@CreationTimestamp
		private Date createdOn;

		//Getters and setters are omitted for brevity
	//end::envers-tracking-properties-changes-mapping-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}
	//tag::envers-tracking-properties-changes-mapping-example[]
	}
	//end::envers-tracking-properties-changes-mapping-example[]
}
