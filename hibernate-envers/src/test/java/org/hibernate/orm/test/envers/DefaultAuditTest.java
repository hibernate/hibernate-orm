/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = DefaultAuditTest.Customer.class)
public class DefaultAuditTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::envers-audited-insert-example[]
			Customer customer = new Customer();
			customer.setId(1L);
			customer.setFirstName("John");
			customer.setLastName("Doe");

			entityManager.persist(customer);
			//end::envers-audited-insert-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-audited-update-example[]
			Customer customer = entityManager.find(Customer.class, 1L);
			customer.setLastName("Doe Jr.");
			//end::envers-audited-update-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-audited-delete-example[]
			Customer customer = entityManager.getReference(Customer.class, 1L);
			entityManager.remove(customer);
			//end::envers-audited-delete-example[]
		});

		//tag::envers-audited-revisions-example[]
		List<Number> revisions = scope.fromTransaction( entityManager -> {
			return AuditReaderFactory.get(entityManager).getRevisions(
				Customer.class,
				1L
			);
		});
		//end::envers-audited-revisions-example[]

		scope.inTransaction( entityManager -> {
			//tag::envers-audited-rev1-example[]
			Customer customer = (Customer) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(Customer.class, revisions.get(0))
			.getSingleResult();

			assertEquals("Doe", customer.getLastName());
			//end::envers-audited-rev1-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-audited-rev2-example[]
			Customer customer = (Customer) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(Customer.class, revisions.get(1))
			.getSingleResult();

			assertEquals("Doe Jr.", customer.getLastName());
			//end::envers-audited-rev2-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-audited-rev3-example[]
			try {
				Customer customer = (Customer) AuditReaderFactory
				.get(entityManager)
				.createQuery()
				.forEntitiesAtRevision(Customer.class, revisions.get(2))
				.getSingleResult();

				fail("The Customer was deleted at this revision: " + revisions.get(2));
			}
			catch (NoResultException expected) {
			}
			//end::envers-audited-rev3-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-audited-rev4-example[]
			Customer customer = (Customer) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(
				Customer.class,
				Customer.class.getName(),
				revisions.get(2),
				true)
			.getSingleResult();

			assertEquals(Long.valueOf(1L), customer.getId());
			assertNull(customer.getFirstName());
			assertNull(customer.getLastName());
			assertNull(customer.getCreatedOn());
			//end::envers-audited-rev4-example[]
		});
	}

	//tag::envers-audited-mapping-example[]
	@Audited
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

	//end::envers-audited-mapping-example[]
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
	//tag::envers-audited-mapping-example[]
	}
	//end::envers-audited-mapping-example[]
}
