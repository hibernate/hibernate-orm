/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.tableperclass;

import java.util.List;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

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
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				MixedInheritanceTest.Customer.class,
				MixedInheritanceTest.DomesticCustomer.class,
				MixedInheritanceTest.ForeignCustomer.class,
				MixedInheritanceTest.Person.class
		}
)
@ServiceRegistry
@SessionFactory
public class MixedInheritanceTest {

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

						for ( Customer result : results ) {
							if ( result.getId() == 1 ) {
								assertThat( result, instanceOf( DomesticCustomer.class ) );
								final DomesticCustomer customer = (DomesticCustomer) result;
								assertThat( customer.getName(), is( "domestic" ) );
								assertThat( ( customer ).getTaxId(), is( "123" ) );
							}
							else if ( result.getId() == 2 ) {
								assertThat( result.getId(), is( 2 ) );
								final ForeignCustomer customer = (ForeignCustomer) result;
								assertThat( customer.getName(), is( "foreign" ) );
								assertThat( ( customer ).getVat(), is( "987" ) );
							}
							else {
								assertThat( result.getId(), is( 3 ) );
								final Customer customer = result;
								assertThat( customer.getName(), is( "customer" ) );
							}
						}

					}
				}
		);
	}

	@Test
	public void rootQueryExecutionTest2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					{
						// [name, taxId, vat]
						final List<Person> results = session.createQuery(
								"select p from Person p",
								Person.class
						).list();

						assertThat( results.size(), is( 4 ) );

						for ( Person result : results ) {
							if ( result.getId() == 1 ) {
								assertThat( result, instanceOf( DomesticCustomer.class ) );
								final DomesticCustomer customer = (DomesticCustomer) result;
								assertThat( customer.getName(), is( "domestic" ) );
								assertThat( ( customer ).getTaxId(), is( "123" ) );
							}
							else if ( result.getId() == 2 ) {
								assertThat( result.getId(), is( 2 ) );
								final ForeignCustomer customer = (ForeignCustomer) result;
								assertThat( customer.getName(), is( "foreign" ) );
								assertThat( ( customer ).getVat(), is( "987" ) );
							}
							else if ( result.getId() == 3 ) {
								final Customer customer = (Customer) result;
								assertThat( customer.getName(), is( "customer" ) );
							}
							else {
								assertThat( result.getId(), is( 4 ) );
								Person person = result;
								assertThat( person.getAge(), is( 23 ) );
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
					Person person = new Person( 4 );
					person.setAge( 23 );
					session.persist( person );

					session.persist( new Customer( 3, "customer" ) );
					session.persist( new DomesticCustomer( 1, "domestic", "123" ) );
					session.persist( new ForeignCustomer( 2, "foreign", "987" ) );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Person {
		private Integer id;

		private int age;

		Person() {
		}

		public Person(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	@Entity(name = "Customer")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Table(name = "customer")
	@DiscriminatorColumn(name = "cust_type")
	public static class Customer extends Person {

		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			super( id );
			this.name = name;
		}


		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "DomesticCustomer")
	@DiscriminatorValue("dc")
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
	@DiscriminatorValue("fc")
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

}
