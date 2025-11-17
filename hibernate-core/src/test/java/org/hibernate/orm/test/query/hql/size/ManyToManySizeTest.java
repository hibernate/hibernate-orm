/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.size;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Imported;
import org.hibernate.community.dialect.DerbyDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@JiraKey( value = "HHH-13619" )
@DomainModel( annotatedClasses = { ManyToManySizeTest.Company.class, ManyToManySizeTest.Customer.class, ManyToManySizeTest.CompanyDto.class } )
@SessionFactory
public class ManyToManySizeTest {

	@Test
	public void testSizeAsRestriction(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List results = session.createQuery(
							"select c.id from Company c where size( c.customers ) = 0"
					).list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( 0 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't see that the subquery is functionally dependent" )
	public void testSizeAsCompoundSelectExpression(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List results = session.createQuery(
							"select c.id, c.name, size( c.customers )" +
									" from Company c" +
									" group by c.id, c.name" +
									" order by c.id"
					).list();
					assertThat( results.size(), is( 3 ) );

					final Object[] first = (Object[]) results.get( 0 );
					assertThat( first[ 0 ], is( 0 ) );
					assertThat( first[ 2 ], is( 0 ) );

					final Object[] second = (Object[]) results.get( 1 );
					assertThat( second[ 0 ], is( 1 ) );
					assertThat( second[ 2 ], is( 1 ) );

					final Object[] third = (Object[]) results.get( 2 );
					assertThat( third[ 0 ], is( 2 ) );
					assertThat( third[ 2 ], is( 2 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't see that the subquery is functionally dependent" )
	public void testSizeAsCtorSelectExpression(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List results = session.createQuery(
							"select new ManyToManySizeTest$CompanyDto(" +
									" c.id, c.name, size( c.customers ) )" +
									" from Company c" +
									" group by c.id, c.name" +
									" order by c.id"
					).list();
					assertThat( results.size(), is( 3 ) );
					final CompanyDto companyDto0 = (CompanyDto) results.get( 0 );
					assertThat( companyDto0.getId(), is( 0 ) );
					assertThat( companyDto0.getName(), is( "Company 0") );
					assertThat( companyDto0.getSizeCustomer(), is( 0 ) );
					final CompanyDto companyDto1 = (CompanyDto) results.get( 1 );
					assertThat( companyDto1.getId(), is( 1 ) );
					assertThat( companyDto1.getName(), is( "Company 1") );
					assertThat( companyDto1.getSizeCustomer(), is( 1 ) );
					final CompanyDto companyDto2 = (CompanyDto) results.get( 2 );
					assertThat( companyDto2.getId(), is( 2 ) );
					assertThat( companyDto2.getName(), is( "Company 2") );
					assertThat( companyDto2.getSizeCustomer(), is( 2 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't see that the subquery is functionally dependent" )
	public void testSizeAsSelectExpressionWithLeftJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List results = session.createQuery(
							"select new ManyToManySizeTest$CompanyDto(" +
									" c.id, c.name, size( c.customers ) )" +
									" from Company c left join c.customers cu" +
									" group by c.id, c.name" +
									" order by c.id"
					).list();
					assertThat( results.size(), is( 3 ) );
					final CompanyDto companyDto0 = (CompanyDto) results.get( 0 );
					assertThat( companyDto0.getId(), is( 0 ) );
					assertThat( companyDto0.getName(), is( "Company 0") );
					assertThat( companyDto0.getSizeCustomer(), is( 0 ) );
					final CompanyDto companyDto1 = (CompanyDto) results.get( 1 );
					assertThat( companyDto1.getId(), is( 1 ) );
					assertThat( companyDto1.getName(), is( "Company 1") );
					assertThat( companyDto1.getSizeCustomer(), is( 1 ) );
					final CompanyDto companyDto2 = (CompanyDto) results.get( 2 );
					assertThat( companyDto2.getId(), is( 2 ) );
					assertThat( companyDto2.getName(), is( "Company 2") );
					assertThat( companyDto2.getSizeCustomer(), is( 2 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't see that the subquery is functionally dependent" )
	public void testSizeAsSelectExpressionWithInnerJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List results = session.createQuery(
							"select new ManyToManySizeTest$CompanyDto(" +
									" c.id, c.name, size( c.customers ) )" +
									" from Company c inner join c.customers cu" +
									" group by c.id, c.name" +
									" order by c.id"
					).list();
					assertThat( results.size(), is( 2 ) );
					final CompanyDto companyDto1 = (CompanyDto) results.get( 0 );
					assertThat( companyDto1.getId(), is( 1 ) );
					assertThat( companyDto1.getName(), is( "Company 1") );
					assertThat( companyDto1.getSizeCustomer(), is( 1 ) );
					final CompanyDto companyDto2 = (CompanyDto) results.get( 1 );
					assertThat( companyDto2.getId(), is( 2 ) );
					assertThat( companyDto2.getName(), is( "Company 2") );
					assertThat( companyDto2.getSizeCustomer(), is( 2 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't see that the subquery is functionally dependent" )
	public void testSizeAsSelectExpressionOfAliasWithInnerJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List results = session.createQuery(
							"select new ManyToManySizeTest$CompanyDto(" +
									" c.id, c.name, size( cu ) )" +
									" from Company c inner join c.customers cu" +
									" group by c.id, c.name" +
									" order by c.id"
					).list();
					assertThat( results.size(), is( 2 ) );
					final CompanyDto companyDto1 = (CompanyDto) results.get( 0 );
					assertThat( companyDto1.getId(), is( 1 ) );
					assertThat( companyDto1.getName(), is( "Company 1") );
					assertThat( companyDto1.getSizeCustomer(), is( 1 ) );
					final CompanyDto companyDto2 = (CompanyDto) results.get( 1 );
					assertThat( companyDto2.getId(), is( 2 ) );
					assertThat( companyDto2.getName(), is( "Company 2") );
					assertThat( companyDto2.getSizeCustomer(), is( 2 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't see that the subquery is functionally dependent" )
	public void testSizeAsSelectExpressionExcludeEmptyCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new ManyToManySizeTest$CompanyDto(" +
									" c.id, c.name, size( c.customers ) )" +
									" from Company c" +
									" where c.id != 0" +
									" group by c.id, c.name order by c.id"
					).list();
					assertThat( results.size(), is( 2 ) );
					final CompanyDto companyDto1 = (CompanyDto) results.get( 0 );
					assertThat( companyDto1.getId(), is( 1 ) );
					assertThat( companyDto1.getName(), is( "Company 1") );
					assertThat( companyDto1.getSizeCustomer(), is( 1 ) );
					final CompanyDto companyDto2 = (CompanyDto) results.get( 1 );
					assertThat( companyDto2.getId(), is( 2 ) );
					assertThat( companyDto2.getName(), is( "Company 2") );
					assertThat( companyDto2.getSizeCustomer(), is( 2 ) );
				}
		);
	}

	@Test
	public void testSizeAsConditionalExpressionExcludeEmptyCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List<Company> results = session.createQuery(
							"from Company c" +
									" where size( c.customers ) > 0" +
									" group by c.id, c.name order by c.id",
							Company.class
					).list();
					assertThat( results.size(), is( 2 ) );
					final Company company1 = results.get( 0 );
					assertThat( company1.id, is( 1 ) );
					assertThat( company1.name, is( "Company 1") );
					assertThat( Hibernate.isInitialized( company1.customers ), is( true ) );
					assertThat( company1.customers.size(), is( 1 ) );
					final Company company2 = results.get( 1 );
					assertThat( company2.id, is( 2 ) );
					assertThat( company2.name, is( "Company 2") );
					assertThat( Hibernate.isInitialized( company2.customers ), is( true ) );
					assertThat( company2.customers.size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testSizeAsConditionalExpressionIncludeEmptyCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List<Company> results = session.createQuery(
							"from Company c" +
									" where size( c.customers ) > -1" +
									" group by c.id, c.name order by c.id",
							Company.class
					).list();
					assertThat( results.size(), is( 3 ) );
					final Company company0 = results.get( 0 );
					assertThat( company0.id, is( 0 ) );
					assertThat( company0.name, is( "Company 0") );
					assertThat( Hibernate.isInitialized(company0.customers), is( true ) );
					assertThat( company0.customers.size(), is( 0 ) );
					final Company company1 = results.get( 1 );
					assertThat( company1.id, is( 1 ) );
					assertThat( company1.name, is( "Company 1") );
					assertThat( Hibernate.isInitialized(company1.customers), is( true ) );
					assertThat( company1.customers.size(), is( 1 ) );
					final Company company2 = results.get( 2 );
					assertThat( company2.id, is( 2 ) );
					assertThat( company2.name, is( "Company 2") );
					assertThat( Hibernate.isInitialized(company2.customers), is( true ) );
					assertThat( company2.customers.size(), is( 2 ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// Add a company with no customers
					final Company companyWithNoCustomers = new Company( 0 );
					companyWithNoCustomers.name = "Company 0";
					session.persist( companyWithNoCustomers );
					int k = 0;
					for ( int i = 1; i <= 2; i++ ) {
						final Company company = new Company( i );
						company.name = "Company " + i;

						for ( int j = 1; j <= i; j++ ) {
							final Customer customer = new Customer( k );
							customer.name = "Customer " + k;
							company.customers.add( customer );
							k++;
						}
						session.persist( company );
					}
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name ="Company")
	public static class Company {

		@Id
		private int id;

		private String name;

		@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
		private List<Customer> customers = new ArrayList<>();

		public Company() {
		}

		public Company(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private int id;

		private String name;

		public Customer() {
		}

		public Customer(int id) {
			this.id = id;
		}
	}

	@Imported
	public static class CompanyDto {

		public int id;

		public String name;

		public int sizeCustomer;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getSizeCustomer() {
			return sizeCustomer;
		}

		public void setSizeCustomer(int sizeCustomer) {
			this.sizeCustomer = sizeCustomer;
		}

		public CompanyDto(){}

		public CompanyDto(int id, String name, int sizeCustomer){
			this.id = id;
			this.name = name;
			this.sizeCustomer = sizeCustomer;
		}
	}
}
