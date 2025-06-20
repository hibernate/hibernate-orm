/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.lazytoone.mappedby;

import org.hibernate.Hibernate;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Baseline test for inverse (mappedBy) to-one, using an explicit @LazyToOne(NO_PROXY)
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				InverseToOneExplicitOptionTests.Customer.class, InverseToOneExplicitOptionTests.SupplementalInfo.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class InverseToOneExplicitOptionTests {

	@Test
	public void testOwnerIsProxy(SessionFactoryScope scope) {
		final EntityPersister supplementalInfoDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( SupplementalInfo.class );
		final BytecodeEnhancementMetadata supplementalInfoEnhancementMetadata = supplementalInfoDescriptor.getBytecodeEnhancementMetadata();
		assertThat( supplementalInfoEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		final EntityPersister customerDescriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Customer.class );
		final BytecodeEnhancementMetadata customerEnhancementMetadata = customerDescriptor.getBytecodeEnhancementMetadata();
		assertThat( customerEnhancementMetadata.isEnhancedForLazyLoading(), is( true ) );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				(session) -> {

					// 1) Get a reference to the SupplementalInfo we created
					// 		- at that point there should be no SQL executed
					// 2) Access the SupplementalInfo's id value
					//		- should trigger no SQL
					// 3) Access SupplementalInfo's `something` state
					//		- should trigger loading the "base group" state, which only include `something`.
					//			NOTE: `customer` is not part of this lazy group because we do not know the
					//			Customer PK from this side
					// 4) Access SupplementalInfo's `customer` state
					//		- should trigger load from Customer table

					final SupplementalInfo supplementalInfo = session.byId( SupplementalInfo.class ).getReference( 1 );

					// we should have just the uninitialized SupplementalInfo proxy
					//		- therefore no SQL statements should have been executed
					assertThat( statementInspector.getSqlQueries().size(), is( 0 ) );

					assertThat(
							supplementalInfoEnhancementMetadata.extractLazyInterceptor( supplementalInfo ),
							instanceOf( EnhancementAsProxyLazinessInterceptor.class )
					);

					// access the id - should do nothing with db
					supplementalInfo.getId();
					assertThat( statementInspector.getSqlQueries().size(), is( 0 ) );
					assertThat(
							supplementalInfoEnhancementMetadata.extractLazyInterceptor( supplementalInfo ),
							instanceOf( EnhancementAsProxyLazinessInterceptor.class )
					);

					// this should trigger loading the entity's base state
					supplementalInfo.getSomething();
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
					assertThat(
							supplementalInfoEnhancementMetadata.extractLazyInterceptor( supplementalInfo ),
							instanceOf( LazyAttributeLoadingInterceptor.class )
					);

					// because we do not know the customer FK from the SupplementalInfo side
					// it is considered part of SupplementalInfo's non-base state.
					//
					// here we access customer which triggers a load from customer table
					final Customer customer = supplementalInfo.getCustomer();
					assertThat( statementInspector.getSqlQueries().size(), is( 2 ) );

					assertThat(
							customerEnhancementMetadata.extractLazyInterceptor( customer ),
							instanceOf( LazyAttributeLoadingInterceptor.class )
					);

					// just as above, accessing id should trigger no loads
					customer.getId();
					assertThat( statementInspector.getSqlQueries().size(), is( 2 ) );

					customer.getName();
					assertThat( statementInspector.getSqlQueries().size(), is( 2 ) );
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
		// so that the getter can safely be called outside of a session.
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
		@OneToOne( fetch = LAZY )
		private SupplementalInfo supplementalInfo;

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

		public SupplementalInfo getSupplementalInfo() {
			return supplementalInfo;
		}

		public void setSupplementalInfo(SupplementalInfo supplementalInfo) {
			this.supplementalInfo = supplementalInfo;
		}
	}

	@Entity( name = "SupplementalInfo" )
	@Table( name = "supplemental" )
	public static class SupplementalInfo {
		@Id
		private Integer id;

		@OneToOne( fetch = LAZY, mappedBy = "supplementalInfo", optional = false )
		private Customer customer;

		private String something;

		public SupplementalInfo() {
		}

		public SupplementalInfo(Integer id, Customer customer, String something) {
			this.id = id;
			this.customer = customer;
			this.something = something;

			customer.setSupplementalInfo( this );
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
