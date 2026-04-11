/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				DetachedEntityParameterAutoFlushVersionTest.Trustee.class,
				DetachedEntityParameterAutoFlushVersionTest.Customer.class
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-20320")
public class DetachedEntityParameterAutoFlushVersionTest {

	@Test
	void reproducesDetachedEntityWithNullVersionDuringAutoFlush(SessionFactoryScope scope) {
		Long customerId = scope.fromTransaction( session -> {
			Trustee trustee = new Trustee();
			trustee.setName("trustee");
			session.persist(trustee);

			Customer customer = new Customer();
			customer.setName("customer");
			customer.setTrustee(trustee);
			session.persist(customer);

			return customer.id;
		});

		// Create a detached entity by fetching it in a separate transaction
		// This should naturally create the condition that triggers the bug
		Trustee detachedTrustee = scope.fromTransaction( session ->
				session.createQuery("SELECT c FROM Customer c WHERE c.id = :id", Customer.class)
						.setParameter("id", customerId)
						.getSingleResult()
						.trustee
		);

		// The bug is now fixed - this should work without throwing PropertyValueException
		scope.inTransaction( session -> {
			// Modify a managed entity to trigger auto-flush
			Customer managedCustomer = session.find(Customer.class, customerId);
			managedCustomer.setName("updated");

			// This query should work now - previously failed in 7.2 with PropertyValueException
			List<Customer> customers = session.createQuery(
							"select c from Customer c where c.trustee = :trustee",
							Customer.class
					)
					.setParameter("trustee", detachedTrustee)
					.list();

			assertThat(customers).hasSize(1);
		});
	}

	@Entity(name = "Trustee")
	@Table(name = "detached_param_trustee")
	public static class Trustee {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trustee_seq")
		@SequenceGenerator(name = "trustee_seq", sequenceName = "trustee_seq", allocationSize = 1)
		public Long id;
		@Version
		public Long version;
		public String name;

		public Long getId() {
			return id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Customer")
	@Table(name = "detached_param_customer")
	public static class Customer {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
		@SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 1)
		public Long id;
		public String name;
		@ManyToOne(fetch = FetchType.LAZY)
		public Trustee trustee;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Trustee getTrustee() {
			return trustee;
		}

		public void setTrustee(Trustee trustee) {
			this.trustee = trustee;
		}
	}
}
