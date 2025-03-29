/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
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
@Jpa(annotatedClasses = {
		ValidityStrategyAuditTest.Customer.class
}, integrationSettings = {
		@Setting(name = EnversSettings.AUDIT_STRATEGY, value = "org.hibernate.envers.strategy.ValidityAuditStrategy")
})
public class ValidityStrategyAuditTest {
	protected void addConfigOptions(Map options) {
		//tag::envers-audited-validity-configuration-example[]
		options.put(
			EnversSettings.AUDIT_STRATEGY,
			ValidityAuditStrategy.class.getName()
		);
		//end::envers-audited-validity-configuration-example[]
	}

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
			Customer customer = entityManager.find(Customer.class, 1L);
			customer.setLastName("Doe Jr.");
		});

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.getReference(Customer.class, 1L);
			entityManager.remove(customer);
		});

		List<Number> revisions = scope.fromTransaction( entityManager -> {
			return AuditReaderFactory.get(entityManager).getRevisions(
				Customer.class,
				1L
			);
		});

		scope.inTransaction( entityManager -> {
			Customer customer = (Customer) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(Customer.class, revisions.get(0))
			.getSingleResult();

			assertEquals("Doe", customer.getLastName());
		});

		scope.inTransaction( entityManager -> {
			Customer customer = (Customer) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(Customer.class, revisions.get(1))
			.getSingleResult();

			assertEquals("Doe Jr.", customer.getLastName());
		});

		scope.inTransaction( entityManager -> {
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
		});

		scope.inTransaction( entityManager -> {
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
		});
	}

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
	}
}
