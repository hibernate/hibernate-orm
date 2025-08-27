/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.lazytoone;

import java.math.BigDecimal;
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

import static jakarta.persistence.FetchType.LAZY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for lazy uni-directional to-one (with SELECT fetching) when enhanced proxies are allowed
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneAllowProxyTests.Customer.class,
				ManyToOneAllowProxyTests.Order.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class ManyToOneAllowProxyTests {

	@Test
	public void testOwnerIsProxy(SessionFactoryScope scope) {
		final EntityPersister orderDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Order.class );
		final BytecodeEnhancementMetadata orderEnhancementMetadata = orderDescriptor.getBytecodeEnhancementMetadata();
		assertThat( orderEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		final EntityPersister customerDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class );
		final BytecodeEnhancementMetadata customerEnhancementMetadata = customerDescriptor.getBytecodeEnhancementMetadata();
		assertThat( customerEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

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
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
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
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				(session) -> {
					final Customer customer = new Customer( 1, "Acme Brick" );
					session.persist( customer );
					final Order order = new Order( 1, customer, BigDecimal.ONE );
					session.persist( order );
				}
		);
		statementInspector.clear();
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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
