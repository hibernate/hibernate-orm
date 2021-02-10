/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone.manytoone;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javax.persistence.FetchType.LAZY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.annotations.FetchMode.JOIN;

/**
 * Test for lazy uni-directional to-one (with JOIN fetching) when enhanced proxies are not allowed
 */
@RunWith( BytecodeEnhancerRunner.class)
@EnhancementOptions( lazyLoading = true )
public class JoinFetchedManyToOneDisallowProxyTests extends BaseNonConfigCoreFunctionalTestCase {
	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Customer.class );
		sources.addAnnotatedClass( Order.class );
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, false );
		sqlStatementInterceptor = new SQLStatementInterceptor( ssrb );
	}

	@Test
	public void testMetadata() {
		final EntityPersister orderDescriptor = sessionFactory().getMetamodel().entityPersister( Order.class );
		final BytecodeEnhancementMetadata orderEnhancementMetadata = orderDescriptor.getBytecodeEnhancementMetadata();
		assertThat( orderEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		final EntityPersister customerDescriptor = sessionFactory().getMetamodel().entityPersister( Customer.class );
		final BytecodeEnhancementMetadata customerEnhancementMetadata = customerDescriptor.getBytecodeEnhancementMetadata();
		assertThat( customerEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );
	}

	@Test
	public void testOwnerIsProxy() {
		sqlStatementInterceptor.clear();

		// NOTE : this combination (lazy+join) is interpreted as (eager+join)
		//		- not sure that is the best interpretation.  should probably be (lazy+select)
		//		- may be a regression from my changes

		inTransaction(
				(session) -> {
					final Order order = session.byId( Order.class ).getReference( 1 );

					// we should have an uninitialized proxy -  therefore no SQL statements should have been executed
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 0 ) );

					// we cannot use enhanced proxies, so order should be a HibernateProxy
					assertThat( order, instanceOf( HibernateProxy.class ) );
					assertThat( Hibernate.isInitialized( order ), is( false ) );

					// access the id - should do nothing with db
					order.getId();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 0 ) );
					assertThat( Hibernate.isInitialized( order ), is( false ) );

					// this should trigger initializing the proxy.  because of above NOTE, customer is join fetched
					// as part of this process
					order.getAmount();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( order ), is( true ) );

					// should not trigger a load
					final Customer customer = order.getCustomer();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
					// again, was join-fetched
					assertThat( Hibernate.isInitialized( customer ), is( true ) );

					// should trigger no loads
					customer.getId();
					customer.getName();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
				}
		);
	}

	@Before
	public void createTestData() {
		inTransaction(
				(session) -> {
					final Customer customer = new Customer( 1, "Acme Brick" );
					session.persist( customer );
					final Order order = new Order( 1, customer, BigDecimal.ONE );
					session.persist( order );
				}
		);
	}

	@After
	public void dropTestData() {
		inTransaction(
				(session) -> {
					session.createQuery( "delete Order" ).executeUpdate();
					session.createQuery( "delete Customer" ).executeUpdate();
				}
		);
	}

	@Entity( name = "Customer" )
	@Table( name = "customer" )
	public static class Customer {
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

	@Entity( name = "Order")
	@Table( name = "`order`")
	public static class Order {
		@Id
		private Integer id;
		@ManyToOne( fetch = LAZY )
		@Fetch( JOIN )
		//we want it to behave as if...
		//@LazyToOne( NO_PROXY )
		private Customer customer;
		private BigDecimal amount;

		public Order() {
		}

		public Order(Integer id, Customer customer, BigDecimal amount) {
			this.id = id;
			this.customer = customer;
			this.amount = amount;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
	}
}
