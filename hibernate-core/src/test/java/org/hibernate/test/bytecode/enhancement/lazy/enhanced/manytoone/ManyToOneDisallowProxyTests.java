/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.enhanced.manytoone;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for lazy uni-directional to-one (with SELECT fetching) when enhanced proxies are not allowed
 */
@RunWith( BytecodeEnhancerRunner.class)
@EnhancementOptions( lazyLoading = true )
public class ManyToOneDisallowProxyTests extends BaseNonConfigCoreFunctionalTestCase {
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
	public void testOwnerIsProxy() {
		sqlStatementInterceptor.clear();

		final EntityPersister orderDescriptor = sessionFactory().getMetamodel().entityPersister( Order.class );
		final BytecodeEnhancementMetadata orderEnhancementMetadata = orderDescriptor.getBytecodeEnhancementMetadata();
		assertThat( orderEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		final EntityPersister customerDescriptor = sessionFactory().getMetamodel().entityPersister( Customer.class );
		final BytecodeEnhancementMetadata customerEnhancementMetadata = customerDescriptor.getBytecodeEnhancementMetadata();
		assertThat( customerEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		inTransaction(
				(session) -> {
					final Order order = session.byId( Order.class ).getReference( 1 );

					// we should have just the uninitialized proxy of the owner - and
					// therefore no SQL statements should have been executed
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 0 ) );

					// we cannot use enhanced proxies, so order should be a HibernateProxy
					assertThat( order, instanceOf( HibernateProxy.class ) );
					assertThat( Hibernate.isInitialized( order ), is( false ) );

					// access the id - should do nothing with db
					order.getId();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 0 ) );

					// this should trigger initializing the order proxy
					order.getAmount();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );

					// should not trigger a load - we should have a HibernateProxy representing the customer
					final Customer customer = order.getCustomer();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );
					assertThat( customer, instanceOf( HibernateProxy.class ) );
					assertThat( Hibernate.isInitialized( customer ), is( false ) );

					// just as above, accessing id should trigger no loads
					customer.getId();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 1 ) );

					customer.getName();
					assertThat( sqlStatementInterceptor.getSqlQueries().size(), is( 2 ) );
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
