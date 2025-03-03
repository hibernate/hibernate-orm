/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.enhancement;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.transaction.TransactionUtil.JPATransactionVoidFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test the behavior of bytecode lazy attributes and {@link PreUpdate @PreUpdate} callbacks
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-7573")
@DomainModel( annotatedClasses = { TestLazyPropertyOnPreUpdate.EntityWithCallback.class } )
@SessionFactory
@BytecodeEnhanced
public class TestLazyPropertyOnPreUpdate {
	private static final byte[] INITIAL_VALUE = new byte[] { 0x2A };

	private static boolean UPDATE_IN_PRE_UPDATE;
	private static final byte[] PRE_UPDATE_VALUE = new byte[] { 0x2A, 0x2A, 0x2A, 0x2A };

	private EntityWithCallback entityUnderTest;

	/**
	 * Update the `name` field; the `lazyData` field will remain
	 * {@linkplain LazyPropertyInitializer#UNFETCHED_PROPERTY uninitialized}.
	 */
	@Test
	public void testNoUpdate(SessionFactoryScope scope) {
		byte[] testArray = new byte[] { 0x2A };

		doInJPA( scope::getSessionFactory, new JPATransactionVoidFunction() {
			@Override
			public void accept(EntityManager em) {
				entityUnderTest = em.find( EntityWithCallback.class, 1 );
				entityUnderTest.setName( "updated name" );
				assertFalse( Hibernate.isPropertyInitialized( entityUnderTest, "lazyData" ) );
			}

			@Override
			public void afterTransactionCompletion() {
				assertFalse( Hibernate.isPropertyInitialized( entityUnderTest, "lazyData" ) );
			}
		} );

		checkLazyField( scope, testArray );
	}

	/**
	 * Set UPDATE_FIELD_IN_CALLBACK so that `lazyField` is updated during the pre-update callback.
	 */
	@Test
	public void testPreUpdate(SessionFactoryScope scope) {
		UPDATE_IN_PRE_UPDATE = true;
		doInJPA( scope::getSessionFactory, new JPATransactionVoidFunction() {
			@Override
			public void accept(EntityManager em) {
				entityUnderTest = em.find( EntityWithCallback.class, 1 );
				entityUnderTest.setName( "updated name" );
				assertFalse( Hibernate.isPropertyInitialized( entityUnderTest, "lazyData" ) );
			}

			@Override
			public void afterTransactionCompletion() {
				assertTrue( Hibernate.isPropertyInitialized( entityUnderTest, "lazyData" ) );
			}
		} );

		checkLazyField( scope, PRE_UPDATE_VALUE );
	}

	/**
	 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
	 * PreUpdate annotated callback method and also set the lazyData field directly to testArray1. When we reload we
	 * should get EntityWithLazyProperty.PRE_UPDATE_VALUE.
	 */
	@Test
	public void testPreUpdateOverride(SessionFactoryScope scope) {
		UPDATE_IN_PRE_UPDATE = true;

		scope.inTransaction( em -> {
			entityUnderTest = em.find( EntityWithCallback.class, 1 );
			assertFalse( Hibernate.isPropertyInitialized( entityUnderTest, "lazyData" ) );
			entityUnderTest.setLazyData( INITIAL_VALUE );
			assertTrue( Hibernate.isPropertyInitialized( entityUnderTest, "lazyData" ) );
			entityUnderTest.setName( "updated name" );
		} );

		checkLazyField( scope, PRE_UPDATE_VALUE );
	}

	private void checkLazyField(SessionFactoryScope scope, byte[] expected) {
		// reload the entity and check the lazy value matches what we expect.
		scope.inTransaction( em -> {
			EntityWithCallback testEntity = em.find( EntityWithCallback.class, 1 );
			assertFalse( Hibernate.isPropertyInitialized( testEntity, "lazyData" ) );
			assertArrayEquals( expected, testEntity.lazyData );
			assertTrue( Hibernate.isPropertyInitialized( testEntity, "lazyData" ) );
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) throws Exception {
		EntityPersister ep = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( EntityWithCallback.class );
		assertTrue( ep.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() );


		scope.inTransaction( em -> {
			entityUnderTest = new EntityWithCallback( 1, "initial name", INITIAL_VALUE );
			em.persist( entityUnderTest );
		} );

		checkLazyField( scope, INITIAL_VALUE );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) throws Exception {
		scope.dropData();
	}

	@Entity(name="EntityWithCallback")
	@Table(name="entity_with_callback")
	public static class EntityWithCallback {
		@Id
		private Integer id;
		@Basic(fetch = FetchType.EAGER)
		private String name;
		@Basic(fetch = FetchType.LAZY)
		private byte[] lazyData;

		public EntityWithCallback() {
		}

		public EntityWithCallback(Integer id, String name, byte[] lazyData) {
			this.id = id;
			this.name = name;
			this.lazyData = lazyData;
		}

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

		public byte[] getLazyData() {
			return lazyData;
		}

		public void setLazyData(byte[] lazyData) {
			this.lazyData = lazyData;
		}

		@PreUpdate
		public void onPreUpdate() {
			//Allow the update of the lazy field from within the pre update to check that this does not break things.
			if ( UPDATE_IN_PRE_UPDATE ) {
				this.lazyData = PRE_UPDATE_VALUE;
			}
		}
	}
}
