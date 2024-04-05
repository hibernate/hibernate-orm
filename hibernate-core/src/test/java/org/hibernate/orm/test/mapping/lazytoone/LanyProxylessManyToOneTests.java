/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import java.math.BigDecimal;

import static jakarta.persistence.FetchType.LAZY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Baseline test for uni-directional to-one, using an explicit @LazyToOne(NO_PROXY)
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				LanyProxylessManyToOneTests.Customer.class, LanyProxylessManyToOneTests.Order.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class LanyProxylessManyToOneTests {

	@Test
	public void testLazyManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Order order = session.byId(Order.class).getReference(1);
					assertThat( Hibernate.isPropertyInitialized( order, "customer"), is(false) );
					assertThat( order.customer, nullValue() );
					Customer customer = order.getCustomer();
					assertThat( Hibernate.isPropertyInitialized( order, "customer"), is(true) );
					assertThat( order.customer, notNullValue() );
					assertThat( customer, notNullValue() );
					assertThat( Hibernate.isInitialized(customer), is(false) );
					assertThat( customer.getId(), is(1) );
					assertThat( Hibernate.isInitialized(customer), is(false) );
					assertThat( customer.getName(), is("Acme Brick") );
					assertThat( Hibernate.isInitialized(customer), is(true) );
				}
		);
		scope.inTransaction(
				(session) -> {
					final Order order = session.byId(Order.class).getReference(1);
					assertThat( Hibernate.isPropertyInitialized( order, "customer"), is(false) );
					assertThat( order.customer, nullValue() );
					Customer customer = order.getCustomer();
					assertThat( Hibernate.isPropertyInitialized( order, "customer"), is(true) );
					assertThat( order.customer, notNullValue() );
					assertThat( customer, notNullValue() );
					assertThat( Hibernate.isInitialized(customer), is(false) );
					Hibernate.initialize( customer );
					assertThat( Hibernate.isInitialized(customer), is(true) );
					assertThat( customer.id, is(1) );
					assertThat( customer.name, is("Acme Brick") );
				}
		);
	}

	@Test
	public void testOwnerIsProxy(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getSessionFactory()
				.getSessionFactoryOptions()
				.getStatementInspector();
		final EntityPersister orderDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Order.class );
		final BytecodeEnhancementMetadata orderEnhancementMetadata = orderDescriptor.getBytecodeEnhancementMetadata();
		assertThat( orderEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		final EntityPersister customerDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class );
		final BytecodeEnhancementMetadata customerEnhancementMetadata = customerDescriptor.getBytecodeEnhancementMetadata();
		assertThat( customerEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		scope.inTransaction(
				(session) -> {
					final Order order = session.byId( Order.class ).getReference( 1 );

					// we should have just the uninitialized proxy of the owner - and
					// therefore no SQL statements should have been executed
					assertThat( statementInspector.getSqlQueries().size(), is( 0 ) );

					final BytecodeLazyAttributeInterceptor initialInterceptor = orderEnhancementMetadata.extractLazyInterceptor( order );
					assertThat( initialInterceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					// access the id - should do nothing with db
					order.getId();
					assertThat( statementInspector.getSqlQueries().size(), is( 0 ) );
					assertThat( initialInterceptor, sameInstance( orderEnhancementMetadata.extractLazyInterceptor( order ) ) );

					// this should trigger loading the entity's base state
					order.getAmount();
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
					final BytecodeLazyAttributeInterceptor interceptor = orderEnhancementMetadata.extractLazyInterceptor( order );
					assertThat( initialInterceptor, not( sameInstance( interceptor ) ) );
					assertThat( interceptor, instanceOf( LazyAttributeLoadingInterceptor.class ) );
					final LazyAttributeLoadingInterceptor attrInterceptor = (LazyAttributeLoadingInterceptor) interceptor;
					assertThat( attrInterceptor.hasAnyUninitializedAttributes(), is( false ) );

					// should not trigger a load and the `customer` reference should be an uninitialized enhanced proxy
					final Customer customer = order.getCustomer();
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );

					final BytecodeLazyAttributeInterceptor initialCustomerInterceptor = customerEnhancementMetadata.extractLazyInterceptor( customer );
					assertThat( initialCustomerInterceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					// just as above, accessing id should trigger no loads
					customer.getId();
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
					assertThat( initialCustomerInterceptor, sameInstance( customerEnhancementMetadata.extractLazyInterceptor( customer ) ) );

					customer.getName();
					assertThat( statementInspector.getSqlQueries().size(), is( 2 ) );
					assertThat( customerEnhancementMetadata.extractLazyInterceptor( customer ), instanceOf( LazyAttributeLoadingInterceptor.class ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-14659")
	public void testQueryJoinFetch(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getSessionFactory()
				.getSessionFactoryOptions()
				.getStatementInspector();

		Order order = scope.fromTransaction( (session) -> {
			final Order result = session.createQuery(
							"select o from Order o join fetch o.customer",
							Order.class )
					.uniqueResult();
			assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
			return result;
		} );

		// The "join fetch" should have already initialized the property,
		// so that the getter can safely be called outside of a session.
		assertTrue( Hibernate.isPropertyInitialized( order, "customer" ) );
		// The "join fetch" should have already initialized the associated entity.
		Customer customer = order.getCustomer();
		assertTrue( Hibernate.isInitialized( customer ) );
		assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Customer customer = new Customer( 1, "Acme Brick" );
					session.persist( customer );
					final Order order = new Order( 1, customer, BigDecimal.ONE );
					session.persist( order );
				}
		);
		( (SQLStatementInspector) scope.getSessionFactory().getSessionFactoryOptions()
				.getStatementInspector() ).clear();
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
