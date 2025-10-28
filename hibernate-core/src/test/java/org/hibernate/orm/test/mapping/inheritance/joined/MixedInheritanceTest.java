/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import java.util.List;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				MixedInheritanceTest.Customer.class,
				MixedInheritanceTest.DomesticCustomer.class,
				MixedInheritanceTest.ForeignCustomer.class,
				MixedInheritanceTest.ItalianCustomer.class
		}
)
@ServiceRegistry
@SessionFactory
public class MixedInheritanceTest {
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

		final EntityPersister italianCustomerDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( ItalianCustomer.class );

		assert customerDescriptor instanceof JoinedSubclassEntityPersister;

		assert customerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert !customerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert !customerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );

		assert domesticCustomerDescriptor instanceof JoinedSubclassEntityPersister;

		assert domesticCustomerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert domesticCustomerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert !domesticCustomerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );

		assert foreignCustomerDescriptor instanceof JoinedSubclassEntityPersister;

		assert foreignCustomerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert !foreignCustomerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert foreignCustomerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );

		assert italianCustomerDescriptor instanceof JoinedSubclassEntityPersister;

		assert italianCustomerDescriptor.isTypeOrSuperType( customerDescriptor );
		assert !italianCustomerDescriptor.isTypeOrSuperType( domesticCustomerDescriptor );
		assert italianCustomerDescriptor.isTypeOrSuperType( foreignCustomerDescriptor );
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

						assertThat( results.size(), is( 3 ) );
						boolean foundDomesticCustomer = false;
						boolean foundForeignCustomer = false;
						boolean foundItalianCustomer = false;
						for ( Customer result : results ) {
							if ( result.getId() == 1 ) {
								assertThat( result, instanceOf( DomesticCustomer.class ) );
								final DomesticCustomer customer = (DomesticCustomer) result;
								assertThat( customer.getName(), is( "domestic" ) );
								assertThat( customer.getTaxId(), is( "123" ) );
								foundDomesticCustomer = true;
							}
							else if ( result.getId() == 2 ) {
								assertThat( result.getId(), is( 2 ) );
								final ForeignCustomer customer = (ForeignCustomer) result;
								assertThat( customer.getName(), is( "foreign" ) );
								assertThat( customer.getVat(), is( "987" ) );
								foundForeignCustomer = true;
							}
							else {
								assertThat( result.getId(), is( 3 ) );
								final ItalianCustomer customer = (ItalianCustomer) result;
								assertThat( customer.getName(), is( "italian" ) );
								assertThat( customer.getVat(), is( "100" ) );
								assertThat( customer.getCode(), is( "IT" ) );
								foundItalianCustomer = true;
							}
						}
						assertTrue( foundDomesticCustomer );
						assertTrue( foundForeignCustomer );
						assertTrue( foundItalianCustomer );
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
						final List<ForeignCustomer> results = session.createQuery(
								"select c from ForeignCustomer c",
								ForeignCustomer.class
						).list();

						assertEquals( 2, results.size() );

						for ( ForeignCustomer foreignCustomer : results ) {
							if ( foreignCustomer.getId() == 2 ) {
								assertTrue( foreignCustomer instanceof ForeignCustomer );
								assertThat( foreignCustomer.getName(), is( "foreign" ) );
								assertThat( foreignCustomer.getVat(), is( "987" ) );
							}
							else {
								assertTrue( foreignCustomer instanceof ItalianCustomer );
								ItalianCustomer italianCustomer = (ItalianCustomer) foreignCustomer;
								assertThat( italianCustomer.getId(), is( 3 ) );
								assertThat( italianCustomer.getName(), is( "italian" ) );
								assertThat( italianCustomer.getVat(), is( "100" ) );
								assertThat( italianCustomer.getCode(), is( "IT" ) );

							}
						}

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
					session.persist( new ItalianCustomer( 3, "italian", "100", "IT" ) );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Customer")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "Customer")
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

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "DomesticCustomer")
	@Table(name = "DomesticCustomer")
	public static class DomesticCustomer extends Customer {
		private String taxId;

		public DomesticCustomer() {
		}

		public DomesticCustomer(Integer id, String name, String taxId) {
			super( id, name );
			this.taxId = taxId;
		}

		public String getTaxId() {
			return taxId;
		}

		public void setTaxId(String taxId) {
			this.taxId = taxId;
		}
	}

	@Entity(name = "ForeignCustomer")
	@Table(name = "ForeignCustomer")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "cust_type")
	@DiscriminatorValue("FC")
	public static class ForeignCustomer extends Customer {
		private String vat;

		public ForeignCustomer() {
		}

		public ForeignCustomer(Integer id, String name, String vat) {
			super( id, name );
			this.vat = vat;
		}

		public String getVat() {
			return vat;
		}

		public void setVat(String vat) {
			this.vat = vat;
		}
	}

	@Entity(name = "ItalianCustomer")
	@DiscriminatorValue("IFC")
	public static class ItalianCustomer extends ForeignCustomer {
		private String code;

		public ItalianCustomer() {
		}

		public ItalianCustomer(Integer id, String name, String vat, String code) {
			super( id, name, vat );
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
}
