/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		MultipleEmbeddedGenericsTest.Customer.class,
		MultipleEmbeddedGenericsTest.Invoice.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-16238")
public class MultipleEmbeddedGenericsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer customer = new Customer( "Marco" );
			session.persist( customer );
			final Invoice invoice = new Invoice( 123 );
			session.persist( invoice );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Customer" ).executeUpdate();
			session.createMutationQuery( "delete from Invoice" ).executeUpdate();
		} );
	}

	@Test
	public void testCustomer(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer customer = session.createQuery(
					"from Customer c where c.firstEmbedded.genericPropertyA = '1' and c.secondEmbedded.customerPropertyB = 2",
					Customer.class
			).getSingleResult();
			assertThat( customer.getName() ).isEqualTo( "Marco" );
		} );
	}

	@Test
	public void testCustomerCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Customer> query = cb.createQuery( Customer.class );
			final Root<Customer> root = query.from( Customer.class );
			final Path<CustomerEmbeddableOne> firstEmbedded = root.get( "firstEmbedded" );
			assertThat( firstEmbedded.getJavaType() ).isEqualTo( GenericEmbeddableOne.class );
			assertThat( firstEmbedded.getModel() ).isSameAs( root.getModel().getAttribute( "firstEmbedded" ) );
			assertThat( ( (SqmPath<?>) firstEmbedded ).getResolvedModel().getBindableJavaType() )
					.isEqualTo( CustomerEmbeddableOne.class );
			final Path<CustomerEmbeddableTwo> secondEmbedded = root.get( "secondEmbedded" );
			assertThat( secondEmbedded.getJavaType() ).isEqualTo( GenericEmbeddableTwo.class );
			assertThat( secondEmbedded.getModel() ).isSameAs( root.getModel().getAttribute( "secondEmbedded" ) );
			assertThat( ( (SqmPath<?>) secondEmbedded ).getResolvedModel().getBindableJavaType() )
					.isEqualTo( CustomerEmbeddableTwo.class );
			query.select( root ).where( cb.and(
					cb.equal( firstEmbedded.get( "genericPropertyA" ), "1" ),
					cb.equal( secondEmbedded.get( "customerPropertyB" ), 2 )
			) );
			final Customer customer = session.createQuery( query ).getSingleResult();
			assertThat( customer.getName() ).isEqualTo( "Marco" );
		} );
	}

	@Test
	public void testInvoice(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Invoice invoice = session.createQuery(
					"from Invoice i where i.firstEmbedded.invoicePropertyA = 1 and i.secondEmbedded.genericPropertyB = '2'",
					Invoice.class
			).getSingleResult();
			assertThat( invoice.getSerial() ).isEqualTo( 123 );
		} );
	}

	@Test
	public void testInvoiceCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Invoice> query = cb.createQuery( Invoice.class );
			final Root<Invoice> root = query.from( Invoice.class );
			final Path<InvoiceEmbeddableOne> firstEmbedded = root.get( "firstEmbedded" );
			assertThat( firstEmbedded.getJavaType() ).isEqualTo( GenericEmbeddableOne.class );
			assertThat( firstEmbedded.getModel() ).isSameAs( root.getModel().getAttribute( "firstEmbedded" ) );
			assertThat( ( (SqmPath<?>) firstEmbedded ).getResolvedModel().getBindableJavaType() )
					.isEqualTo( InvoiceEmbeddableOne.class );
			final Path<InvoiceEmbeddableTwo> secondEmbedded = root.get( "secondEmbedded" );
			assertThat( secondEmbedded.getJavaType() ).isEqualTo( GenericEmbeddableTwo.class );
			assertThat( secondEmbedded.getModel() ).isSameAs( root.getModel().getAttribute( "secondEmbedded" ) );
			assertThat( ( (SqmPath<?>) secondEmbedded ).getResolvedModel().getBindableJavaType() )
					.isEqualTo( InvoiceEmbeddableTwo.class );
			query.select( root ).where( cb.and(
					cb.equal( firstEmbedded.get( "invoicePropertyA" ), 1 ),
					cb.equal( secondEmbedded.get( "genericPropertyB" ), "2" )
			) );
			final Invoice invoice = session.createQuery( query ).getSingleResult();
			assertThat( invoice.getSerial() ).isEqualTo( 123 );
		} );
	}

	@Embeddable
	@MappedSuperclass
	public abstract static class GenericEmbeddableOne {
		private String genericPropertyA;

		public GenericEmbeddableOne() {
		}

		public GenericEmbeddableOne(String genericPropertyA) {
			this.genericPropertyA = genericPropertyA;
		}
	}

	@Embeddable
	@MappedSuperclass
	public abstract static class GenericEmbeddableTwo {
		private String genericPropertyB;

		public GenericEmbeddableTwo() {
		}

		public GenericEmbeddableTwo(String genericPropertyB) {
			this.genericPropertyB = genericPropertyB;
		}
	}

	@MappedSuperclass
	public abstract static class GenericEntity<A extends GenericEmbeddableOne, B extends GenericEmbeddableTwo> {
		@Embedded
		private A firstEmbedded;

		@Embedded
		private B secondEmbedded;

		public GenericEntity() {
		}

		public GenericEntity(A firstEmbedded, B secondEmbedded) {
			this.firstEmbedded = firstEmbedded;
			this.secondEmbedded = secondEmbedded;
		}
	}

	@Embeddable
	public static class CustomerEmbeddableOne extends GenericEmbeddableOne {
		private int customerPropertyA;

		public CustomerEmbeddableOne() {
		}

		public CustomerEmbeddableOne(String genericPropertyA, int customerPropertyA) {
			super( genericPropertyA );
			this.customerPropertyA = customerPropertyA;
		}
	}

	@Embeddable
	public static class CustomerEmbeddableTwo extends GenericEmbeddableTwo {
		private int customerPropertyB;

		public CustomerEmbeddableTwo() {
		}

		public CustomerEmbeddableTwo(String genericPropertyB, int customerPropertyB) {
			super( genericPropertyB );
			this.customerPropertyB = customerPropertyB;
		}
	}

	@Entity(name = "Customer")
	public static class Customer extends GenericEntity<CustomerEmbeddableOne, CustomerEmbeddableTwo> {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Customer() {
		}

		public Customer(String name) {
			super( new CustomerEmbeddableOne( "1", 1 ), new CustomerEmbeddableTwo( "2", 2 ) );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class InvoiceEmbeddableOne extends GenericEmbeddableOne {
		private int invoicePropertyA;

		public InvoiceEmbeddableOne() {
		}

		public InvoiceEmbeddableOne(String genericPropertyA, int invoicePropertyA) {
			super( genericPropertyA );
			this.invoicePropertyA = invoicePropertyA;
		}
	}

	@Embeddable
	public static class InvoiceEmbeddableTwo extends GenericEmbeddableTwo {
		private int invoicePropertyB;

		public InvoiceEmbeddableTwo() {
		}

		public InvoiceEmbeddableTwo(String genericPropertyB, int invoicePropertyB) {
			super( genericPropertyB );
			this.invoicePropertyB = invoicePropertyB;
		}
	}

	@Entity(name = "Invoice")
	public static class Invoice extends GenericEntity<InvoiceEmbeddableOne, InvoiceEmbeddableTwo> {
		@Id
		@GeneratedValue
		private Long id;

		private Integer serial;

		public Invoice() {
		}

		public Invoice(Integer serial) {
			super( new InvoiceEmbeddableOne( "1", 1 ), new InvoiceEmbeddableTwo( "2", 2 ) );
			this.serial = serial;
		}

		public Integer getSerial() {
			return serial;
		}

		public void setSerial(Integer serial) {
			this.serial = serial;
		}
	}
}
