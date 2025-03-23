/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.io.Serializable;
import java.util.Random;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
		EmbeddedIdGenericsSuperclassTest.Customer.class,
		EmbeddedIdGenericsSuperclassTest.Invoice.class,
		EmbeddedIdGenericsSuperclassTest.NestedCustomer.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-16188")
public class EmbeddedIdGenericsSuperclassTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer customer = new Customer( 1, "Francisco" );
			session.persist( customer );
			final Invoice invoice = new Invoice( 2 );
			session.persist( invoice );
			final NestedCustomer nestedCustomer = new NestedCustomer( "Marco" );
			session.persist( nestedCustomer );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Customer" ).executeUpdate();
			session.createMutationQuery( "delete from Invoice" ).executeUpdate();
			session.createMutationQuery( "delete from NestedCustomer" ).executeUpdate();
		} );
	}

	@Test
	public void testCustomer(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer customer = session.createQuery(
					"from Customer c where c.id.someDomainField = 1",
					Customer.class
			).getSingleResult();
			assertThat( customer ).isNotNull();
			assertThat( customer.getCode() ).isEqualTo( 1 );
			assertThat( customer.getName() ).isEqualTo( "Francisco" );
		} );
	}

	@Test
	public void testCustomerCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Customer> query = cb.createQuery( Customer.class );
			final Root<Customer> root = query.from( Customer.class );
			final Path<DomainEntityId> id = root.get( "id" );
			assertThat( id.getJavaType() ).isEqualTo( DomainEntityId.class );
			assertThat( id.getModel() ).isSameAs( root.getModel().getAttribute( "id" ) );
			assertThat( ( (SqmPath<?>) id ).getResolvedModel().getBindableJavaType() ).isEqualTo( CustomerId.class );
			query.select( root ).where( cb.equal( id.get( "someDomainField" ), 1 ) );
			final Customer customer = session.createQuery( query ).getSingleResult();
			assertThat( customer ).isNotNull();
			assertThat( customer.getCode() ).isEqualTo( 1 );
			assertThat( customer.getName() ).isEqualTo( "Francisco" );
		} );
	}

	@Test
	public void testInvoice(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Invoice invoice = session.createQuery(
					"from Invoice a where a.id.someOtherDomainField = 1",
					Invoice.class
			).getSingleResult();
			assertThat( invoice ).isNotNull();
			assertThat( invoice.getSerial() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testInvoiceCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Invoice> query = cb.createQuery( Invoice.class );
			final Root<Invoice> root = query.from( Invoice.class );
			final Path<DomainEntityId> id = root.get( "id" );
			assertThat( id.getJavaType() ).isEqualTo( DomainEntityId.class );
			assertThat( id.getModel() ).isSameAs( root.getModel().getAttribute( "id" ) );
			assertThat( ( (SqmPath<?>) id ).getResolvedModel().getBindableJavaType() ).isEqualTo( InvoiceId.class );
			query.select( root ).where( cb.equal( id.get( "someOtherDomainField" ), 1 ) );
			final Invoice invoice = session.createQuery( query ).getSingleResult();
			assertThat( invoice ).isNotNull();
			assertThat( invoice.getSerial() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testNestedCustomer(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final NestedCustomer nestedCustomer = session.createQuery(
					"from NestedCustomer c where c.id.nestedEmbeddable.someNestedDomainField = 1 and c.id.someFirstLevelDomainField = 1",
					NestedCustomer.class
			).getSingleResult();
			assertThat( nestedCustomer ).isNotNull();
			assertThat( nestedCustomer.getName() ).isEqualTo( "Marco" );
		} );
	}

	@Embeddable
	@MappedSuperclass
	public abstract static class DomainEntityId implements Serializable {
		private Long domainId;

		public DomainEntityId() {
			Random random = new Random();
			this.domainId = random.nextLong();
		}
	}

	@MappedSuperclass
	public abstract static class DomainEntityModel<ID extends DomainEntityId> {
		@EmbeddedId
		private ID id;

		protected DomainEntityModel(ID id) {
			this.id = id;
		}

		public ID getId() {
			return id;
		}
	}

	@Embeddable
	public static class CustomerId extends DomainEntityId {
		private int someDomainField;

		public CustomerId() {
			super();
			this.someDomainField = 1;
		}
	}

	@Entity(name = "Customer")
	public static class Customer extends DomainEntityModel<CustomerId> {
		private Integer code;
		private String name;

		public Customer() {
			super( new CustomerId() );
		}

		public Customer(Integer code, String name) {
			this();
			this.code = code;
			this.name = name;
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
	}

	@Embeddable
	public static class InvoiceId extends DomainEntityId {
		private int someOtherDomainField;

		public InvoiceId() {
			super();
			this.someOtherDomainField = 1;
		}
	}

	@Entity(name = "Invoice")
	public static class Invoice extends DomainEntityModel<InvoiceId> {
		private Integer serial;

		public Invoice() {
			super( new InvoiceId() );
		}

		public Invoice(Integer serial) {
			this();
			this.serial = serial;
		}

		public Integer getSerial() {
			return serial;
		}

		public void setSerial(Integer serial) {
			this.serial = serial;
		}
	}

	@Embeddable
	public static class NestedEmbeddable {
		private int someNestedDomainField;

		public NestedEmbeddable() {
		}

		public NestedEmbeddable(int someNestedDomainField) {
			this.someNestedDomainField = someNestedDomainField;
		}
	}

	@Embeddable
	public static class NestedId extends DomainEntityId {
		private NestedEmbeddable nestedEmbeddable;

		private int someFirstLevelDomainField;

		public NestedId() {
			super();
			this.nestedEmbeddable = new NestedEmbeddable( 1 );
			this.someFirstLevelDomainField = 1;
		}
	}

	@Entity(name = "NestedCustomer")
	public static class NestedCustomer extends DomainEntityModel<NestedId> {
		private String name;

		public NestedCustomer() {
			super( new NestedId() );
		}

		public NestedCustomer(String name) {
			this();
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
