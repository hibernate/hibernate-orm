/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.Random;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		OneToOneEmbeddedIdWithGenericsTest.Customer.class,
		OneToOneEmbeddedIdWithGenericsTest.Invoice.class
})
@SessionFactory
@JiraKey("HHH-16070")
public class OneToOneEmbeddedIdWithGenericsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Customer customer = new Customer( 1, "Francisco" );
			Invoice invoice = new Invoice( 1, customer );
			customer.setInvoice( invoice );
			session.persist( customer );
			session.persist( invoice );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Invoice" ).executeUpdate();
			session.createMutationQuery( "delete from Customer" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Customer customer = session.createQuery( "from Customer", Customer.class ).getSingleResult();
			assertNotNull( customer );
			Invoice invoice = session.createQuery(
							"from Invoice i where i.customer.id = :customerId",
							Invoice.class
					)
					.setParameter( "customerId", customer.getId() )
					.getSingleResult();
			assertNotNull( invoice );
		} );
	}

	@Test
	public void testInverse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Invoice invoice = session.createQuery( "from Invoice", Invoice.class ).getSingleResult();
			assertNotNull( invoice );
			Customer customer = session.createQuery(
							"from Customer c where c.invoice.id = :invoiceId",
							Customer.class
					)
					.setParameter( "invoiceId", invoice.getId() )
					.getSingleResult();
			assertNotNull( customer );
		} );
	}

	@MappedSuperclass
	public abstract static class DomainEntityId implements Serializable {
		@Column(name = "id_value")
		private final Long value;

		protected DomainEntityId() {
			Random random = new Random();
			this.value = random.nextLong();
		}

		public Long getValue() {
			return value;
		}
	}

	@MappedSuperclass
	public abstract static class DomainEntityModel<ID extends DomainEntityId> {
		@EmbeddedId
		private final ID id;

		protected DomainEntityModel(ID id) {
			this.id = id;
		}

		public ID getId() {
			return id;
		}
	}

	@Embeddable
	public static class CustomerId extends DomainEntityId {
	}

	@Entity(name = "Customer")
	@Table(name = "Customer")
	public static class Customer extends DomainEntityModel<CustomerId> {
		private Integer code;
		private String name;

		@OneToOne(mappedBy = "customer")
		private Invoice invoice;

		public Customer(Integer code, String name) {
			this();
			this.code = code;
			this.name = name;
		}

		protected Customer() {
			super( new CustomerId() );
		}

		public Integer getCode() {
			return code;
		}

		public void setCode(Integer code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Invoice getInvoice() {
			return invoice;
		}

		public void setInvoice(Invoice invoice) {
			this.invoice = invoice;
		}
	}

	@Embeddable
	public static class InvoiceId extends DomainEntityId {
	}

	@Entity(name = "Invoice")
	@Table(name = "Invoice")
	public static class Invoice extends DomainEntityModel<InvoiceId> {
		private Integer serial;

		@OneToOne
		private Customer customer;

		public Invoice(Integer serial, Customer customer) {
			this();
			this.serial = serial;
			this.customer = customer;
		}

		protected Invoice() {
			super( new InvoiceId() );
		}

		public Integer getSerial() {
			return serial;
		}

		public void setSerial(Integer serial) {
			this.serial = serial;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}
}
