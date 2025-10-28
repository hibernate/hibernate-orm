/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance;

import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SingleTableInheritanceTests.Customer.class,
				SingleTableInheritanceTests.DomesticCustomer.class,
				SingleTableInheritanceTests.ForeignCustomer.class
		}
)
@ServiceRegistry
@SessionFactory
public class SingleTableInheritanceTests {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		final EntityPersister customerDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( Customer.class );
		final EntityPersister domesticCustomerDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( DomesticCustomer.class );
		final EntityPersister foreignCustomerDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( ForeignCustomer.class );

		assert customerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert ! customerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert ! customerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );

		assert domesticCustomerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert domesticCustomerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert ! domesticCustomerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );

		assert foreignCustomerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert ! foreignCustomerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert foreignCustomerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );
	}

	@Test
	public void rootQueryExecutionTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						// [name, taxId, vat]
						final List<Customer> results = session.createQuery(
								"select c from Customer c",
								Customer.class
						).list();

						assertThat( results.size(), is( 2 ) );

						for ( Customer result : results ) {
							if ( result.getId() == 1 ) {
								assertThat( result, instanceOf( DomesticCustomer.class ) );
								final DomesticCustomer customer = (DomesticCustomer) result;
								assertThat( customer.getName(), is( "domestic" ) );
								assertThat( customer.getTaxId(), is( "123" ) );
							}
							else {
								assertThat( result.getId(), is( 2 ) );
								final ForeignCustomer customer = (ForeignCustomer) result;
								assertThat( customer.getName(), is( "foreign" ) );
								assertThat( customer.getVat(), is( "987" ) );
							}
						}

					}
				}
		);
	}

	@Test
	public void subclassQueryExecutionTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						final DomesticCustomer result = session.createQuery(
								"select c from DomesticCustomer c",
								DomesticCustomer.class
						).uniqueResult();

						assertThat( result, notNullValue() );
						assertThat( result.getId(), is( 1 ) );
						assertThat( result.getName(), is( "domestic" ) );
						assertThat( result.getTaxId(), is( "123" ) );
					}

					{
						final ForeignCustomer result = session.createQuery(
								"select c from ForeignCustomer c",
								ForeignCustomer.class
						).uniqueResult();

						assertThat( result, notNullValue() );
						assertThat( result.getId(), is( 2 ) );
						assertThat( result.getName(), is( "foreign" ) );
						assertThat( result.getVat(), is( "987" ) );
					}
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new DomesticCustomer( 1, "domestic", "123" ) );
					session.persist( new ForeignCustomer( 2, "foreign", "987" ) );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Customer" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@Table( name = "customer" )
	@DiscriminatorColumn( name = "cust_type" )
	public static abstract class Customer {
		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Basic( optional = false )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "DomesticCustomer" )
	@DiscriminatorValue( "dc" )
	public static class DomesticCustomer extends Customer {
		private String taxId;

		public DomesticCustomer() {
		}

		public DomesticCustomer(Integer id, String name, String taxId) {
			super( id, name );
			this.taxId = taxId;
		}

		@Basic( optional = false )
		public String getTaxId() {
			return taxId;
		}

		public void setTaxId(String taxId) {
			this.taxId = taxId;
		}
	}

	@Entity( name = "ForeignCustomer" )
	@DiscriminatorValue( "fc" )
	public static class ForeignCustomer extends Customer {
		private String vat;

		public ForeignCustomer() {
		}

		public ForeignCustomer(Integer id, String name, String vat) {
			super( id, name );
			this.vat = vat;
		}

		@Basic( optional = false )
		public String getVat() {
			return vat;
		}

		public void setVat(String vat) {
			this.vat = vat;
		}
	}

}
