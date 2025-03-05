/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;

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
		OneToOneEmbeddedIdWithGenericAttributeTest.Customer.class,
		OneToOneEmbeddedIdWithGenericAttributeTest.Invoice.class
})
@SessionFactory
@JiraKey("HHH-16070")
@JiraKey("HHH-16195")
public class OneToOneEmbeddedIdWithGenericAttributeTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer customer = new Customer( 1, "Francisco" );
			final Invoice invoice = new Invoice( 1, customer );
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
		final Customer customer = scope.fromTransaction( session -> session.createQuery(
				"from Customer",
				Customer.class
		).getSingleResult() );
		assertNotNull( customer );

		scope.inTransaction( session -> {
			final Invoice invoice = session.createQuery(
							"from Invoice i where i.customer.id = :customerId",
							Invoice.class
					)
					.setParameter( "customerId", customer.getId() )
					.getSingleResult();
			assertNotNull( invoice );
		} );

		scope.inTransaction( session -> {
			final Invoice invoice = session.createQuery(
							"from Invoice i where i.customer.id.value = :idValue",
							Invoice.class
					)
					.setParameter( "idValue", customer.getId().getValue() )
					.getSingleResult();
			assertNotNull( invoice );
		} );
	}

	@Test
	public void testInverse(SessionFactoryScope scope) {
		final Invoice invoice = scope.fromTransaction( session -> session.createQuery(
				"from Invoice",
				Invoice.class
		).getSingleResult() );
		assertNotNull( invoice );

		scope.inTransaction( session -> {
			final Customer customer = session.createQuery(
							"from Customer c where c.invoice.id = :invoiceId",
							Customer.class
					)
					.setParameter( "invoiceId", invoice.getId() )
					.getSingleResult();
			assertNotNull( customer );
		} );

		scope.inTransaction( session -> {
			final Customer customer = session.createQuery(
							"from Customer c where c.invoice.id.value = :idValue",
							Customer.class
					)
					.setParameter( "idValue", invoice.getId().getValue() )
					.getSingleResult();
			assertNotNull( customer );
		} );
	}

	@Embeddable
	public static class DomainEntityId<T> implements Serializable {
		@Column(name = "id_value")
		private T value;

		public DomainEntityId() {
		}

		public DomainEntityId(T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}
	}

	@MappedSuperclass
	public abstract static class DomainEntityModel<T> {
		@EmbeddedId
		private DomainEntityId<T> id;

		public DomainEntityModel() {
		}

		protected DomainEntityModel(DomainEntityId<T> id) {
			this.id = id;
		}

		public DomainEntityId<T> getId() {
			return id;
		}

		public void setId(DomainEntityId<T> id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class CustomerId extends DomainEntityId<String> {
		public CustomerId() {
		}

		public CustomerId(String value) {
			super( value );
		}
	}

	@Entity(name = "Customer")
	@Table(name = "Customer")
	public static class Customer extends DomainEntityModel<String> {
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
			super( new CustomerId( "customer" ) );
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
	public static class InvoiceId extends DomainEntityId<Integer> {
		public InvoiceId() {
		}

		public InvoiceId(Integer value) {
			super( value );
		}
	}

	@Entity(name = "Invoice")
	@Table(name = "Invoice")
	public static class Invoice extends DomainEntityModel<Integer> {
		@OneToOne
		private Customer customer;

		public Invoice() {
			super();
		}

		public Invoice(Integer serial, Customer customer) {
			super( new InvoiceId( serial ) );
			this.customer = customer;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}
}
