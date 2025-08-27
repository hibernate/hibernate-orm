/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.lazytoone.onetoone;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Baseline test for uni-directional one-to-one, using an explicit @LazyToOne(NO_PROXY) and allowing enhanced proxies
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				OneToOneExplicitOptionTests.Customer.class, OneToOneExplicitOptionTests.SupplementalInfo.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class OneToOneExplicitOptionTests {

	@Test
	public void testLazyOneToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SupplementalInfo supplementalInfo = session.byId(SupplementalInfo.class).getReference(1);
					assertThat( Hibernate.isPropertyInitialized( supplementalInfo, "customer"), is(false) );
					assertThat( supplementalInfo.customer, nullValue() );
					Customer customer = supplementalInfo.getCustomer();
					assertThat( Hibernate.isPropertyInitialized( supplementalInfo, "customer"), is(true) );
					assertThat( supplementalInfo.customer, notNullValue() );
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
					final SupplementalInfo supplementalInfo = session.byId(SupplementalInfo.class).getReference(1);
					assertThat( Hibernate.isPropertyInitialized( supplementalInfo, "customer"), is(false) );
					assertThat( supplementalInfo.customer, nullValue() );
					Customer customer = supplementalInfo.getCustomer();
					assertThat( Hibernate.isPropertyInitialized( supplementalInfo, "customer"), is(true) );
					assertThat( supplementalInfo.customer, notNullValue() );
					assertThat( customer, notNullValue() );
					assertThat( Hibernate.isInitialized(customer), is(false) );
					Hibernate.initialize( customer );
					assertThat( Hibernate.isInitialized(customer), is(true) );
					assertThat( customer.getId(), is(1) );
					assertThat( customer.getName(), is("Acme Brick") );
				}
		);
	}

	@Test
	public void testOwnerIsProxy(SessionFactoryScope scope) {
		final EntityPersister supplementalInfoDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( SupplementalInfo.class );
		final BytecodeEnhancementMetadata supplementalInfoEnhancementMetadata = supplementalInfoDescriptor.getBytecodeEnhancementMetadata();
		assertThat( supplementalInfoEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		final EntityPersister customerDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class );
		final BytecodeEnhancementMetadata customerEnhancementMetadata = customerDescriptor.getBytecodeEnhancementMetadata();
		assertThat( customerEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				(session) -> {
					final SupplementalInfo supplementalInfo = session.byId( SupplementalInfo.class ).getReference( 1 );

					// we should have just the uninitialized SupplementalInfo proxy
					//		- therefore no SQL statements should have been executed
					assertThat( sqlStatementInspector.getSqlQueries().size(), is( 0 ) );

					final BytecodeLazyAttributeInterceptor initialInterceptor = supplementalInfoEnhancementMetadata.extractLazyInterceptor( supplementalInfo );
					assertThat( initialInterceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					// access the id - should do nothing with db
					supplementalInfo.getId();
					assertThat( sqlStatementInspector.getSqlQueries().size(), is( 0 ) );
					assertThat( supplementalInfoEnhancementMetadata.extractLazyInterceptor( supplementalInfo ), sameInstance( initialInterceptor ) );

					// this should trigger loading the entity's base state
					supplementalInfo.getSomething();
					assertThat( sqlStatementInspector.getSqlQueries().size(), is( 1 ) );
					final BytecodeLazyAttributeInterceptor interceptor = supplementalInfoEnhancementMetadata.extractLazyInterceptor( supplementalInfo );
					assertThat( initialInterceptor, not( sameInstance( interceptor ) ) );
					assertThat( interceptor, instanceOf( LazyAttributeLoadingInterceptor.class ) );
					final LazyAttributeLoadingInterceptor attrInterceptor = (LazyAttributeLoadingInterceptor) interceptor;
					assertThat( attrInterceptor.hasAnyUninitializedAttributes(), is( false ) );

					// should not trigger a load and the `customer` reference should be an uninitialized enhanced proxy
					final Customer customer = supplementalInfo.getCustomer();
					assertThat( sqlStatementInspector.getSqlQueries().size(), is( 1 ) );

					final BytecodeLazyAttributeInterceptor initialCustomerInterceptor = customerEnhancementMetadata.extractLazyInterceptor( customer );
					assertThat( initialCustomerInterceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					// just as above, accessing id should trigger no loads
					customer.getId();
					assertThat( sqlStatementInspector.getSqlQueries().size(), is( 1 ) );
					assertThat( initialCustomerInterceptor, sameInstance( customerEnhancementMetadata.extractLazyInterceptor( customer ) ) );

					customer.getName();
					assertThat( sqlStatementInspector.getSqlQueries().size(), is( 2 ) );
					assertThat( customerEnhancementMetadata.extractLazyInterceptor( customer ), instanceOf( LazyAttributeLoadingInterceptor.class ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-14659")
	public void testQueryJoinFetch(SessionFactoryScope scope) {
		SupplementalInfo info = scope.fromTransaction( (session) -> {
			final SupplementalInfo result = session.createQuery(
							"select s from SupplementalInfo s join fetch s.customer",
							SupplementalInfo.class )
					.uniqueResult();
			assertThat( scope.getCollectingStatementInspector().getSqlQueries().size(), is( 1 ) );
			return result;
		} );

		// The "join fetch" should have already initialized the property,
		// so that the getter can safely be called outside a session.
		assertTrue( Hibernate.isPropertyInitialized( info, "customer" ) );
		// The "join fetch" should have already initialized the associated entity.
		Customer customer = info.getCustomer();
		assertTrue( Hibernate.isInitialized( customer ) );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().size(), is( 1 ) );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Customer customer = new Customer( 1, "Acme Brick" );
					session.persist( customer );
					final SupplementalInfo supplementalInfo = new SupplementalInfo( 1, customer, "extra details" );
					session.persist( supplementalInfo );
				}
		);
		scope.getCollectingStatementInspector().clear();
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

	@Entity( name = "SupplementalInfo" )
	@Table( name = "supplemental" )
	public static class SupplementalInfo {
		@Id
		private Integer id;

		@OneToOne( fetch = LAZY, optional = false )
		private Customer customer;

		private String something;

		public SupplementalInfo() {
		}

		public SupplementalInfo(Integer id, Customer customer, String something) {
			this.id = id;
			this.customer = customer;
			this.something = something;
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

		public String getSomething() {
			return something;
		}

		public void setSomething(String something) {
			this.something = something;
		}
	}
}
