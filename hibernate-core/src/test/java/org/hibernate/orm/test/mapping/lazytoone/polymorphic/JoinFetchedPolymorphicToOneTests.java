/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone.polymorphic;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.annotations.FetchMode.JOIN;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				JoinFetchedPolymorphicToOneTests.Order.class,
				JoinFetchedPolymorphicToOneTests.Customer.class,
				JoinFetchedPolymorphicToOneTests.ForeignCustomer.class,
				JoinFetchedPolymorphicToOneTests.DomesticCustomer.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class JoinFetchedPolymorphicToOneTests {

	@Test
	public void testInheritedToOneLaziness(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();
					sqlStatementInspector.clear();

					final Order order = session.byId( Order.class ).getReference( 1 );
					sqlStatementInspector.assertExecutedCount( 0 );

					System.out.println( "Order # " + order.getId() );
					sqlStatementInspector.assertExecutedCount( 0 );

					System.out.println( "  - amount : " + order.getAmount() );
					// triggers load of base fetch state
					sqlStatementInspector.assertExecutedCount( 1 );

					final Customer customer = order.getCustomer();
					// customer is part of base fetch state
					sqlStatementInspector.assertExecutedCount( 1 );
					assertTrue( Hibernate.isInitialized( customer ) );

					System.out.println( "  - customer : " + customer.getId() );
					sqlStatementInspector.assertExecutedCount( 1 );

					customer.getName();
					// customer base fetch state should also have been loaded above
					sqlStatementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-14659")
	public void testQueryJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Order order = session.createQuery( "select o from Order o join fetch o.customer", Order.class )
							.uniqueResult();

					assertTrue( Hibernate.isPropertyInitialized( order, "customer" ) );
					Customer customer = order.getCustomer();
					assertTrue( Hibernate.isInitialized( customer ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final DomesticCustomer customer = new DomesticCustomer( 1, "them", "123" );
					session.persist( customer );
					final Order order = new Order( 1, BigDecimal.ONE, customer );
					session.persist( order );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "delete Order" ).executeUpdate();
					session.createQuery( "delete Customer" ).executeUpdate();
				}
		);
	}

	@Entity( name = "Order" )
	@Table( name = "`order`" )
	public static class Order {
		@Id
		private Integer id;
		private BigDecimal amount;
		@ManyToOne( fetch = LAZY, optional = false )
		@Fetch( JOIN )
		private Customer customer;

		public Order() {
		}

		public Order(Integer id, BigDecimal amount, Customer customer) {
			this.id = id;
			this.amount = amount;
			this.customer = customer;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Entity( name = "Customer" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static abstract class Customer {
		@Id
		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ForeignCustomer")
	@Table(name = "foreign_cust")
	public static class ForeignCustomer extends Customer {
		private String vat;

		public ForeignCustomer() {
			super();
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

	@Entity(name = "DomesticCustomer")
	@Table(name = "domestic_cust")
	public static class DomesticCustomer extends Customer {
		private String taxId;

		public DomesticCustomer() {
			super();
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
}
