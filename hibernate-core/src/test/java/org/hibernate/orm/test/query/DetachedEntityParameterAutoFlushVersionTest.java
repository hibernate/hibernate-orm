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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
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
public class DetachedEntityParameterAutoFlushVersionTest {

	@Test
	void detachedEntityParameterShouldNotRequireManagedAssociation(SessionFactoryScope scope) {
		Long trusteeId = scope.fromTransaction( session -> {
			Trustee trustee = new Trustee();
			trustee.setName( "trustee" );
			session.persist( trustee );

			Customer customer = new Customer();
			customer.setName( "customer" );
			customer.setTrustee( trustee );
			session.persist( customer );

			return trustee.getId();
		} );

		// Create a truly detached entity with ID but null version
		Trustee detachedTrustee = new Trustee();
		detachedTrustee.id = trusteeId;
		// version is null (unsaved value) - this should trigger the transient check
		detachedTrustee.name = "trustee";

		scope.inTransaction( session -> {
			Customer managedCustomer = session.createQuery(
					"select c from Customer c",
					Customer.class
			)
					.setMaxResults( 1 )
					.getSingleResult();

			managedCustomer.setTrustee( detachedTrustee );

			List<Customer> customers = session.createQuery(
					"select c from Customer c where c.trustee = :trustee",
					Customer.class
			)
					.setParameter( "trustee", detachedTrustee )
					.getResultList();

			assertThat( customers ).hasSize( 1 );
		} );
	}

	@Entity(name = "Trustee")
	@Table(name = "detached_param_trustee")
	public static class Trustee {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		@Version
		Long version;

		String name;

		public Long getId() {
			return id;
		}

		public Long getVersion() {
			return version;
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
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Trustee trustee;

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
