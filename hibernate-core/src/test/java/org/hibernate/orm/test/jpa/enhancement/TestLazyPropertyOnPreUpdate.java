/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.enhancement;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.Hibernate;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.transaction.TransactionUtil.JPATransactionVoidFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.util.Arrays;


@JiraKey("HHH-7573")
@DomainModel(
		annotatedClasses = {
				TestLazyPropertyOnPreUpdate.EntityWithLazyProperty.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class TestLazyPropertyOnPreUpdate {

	private EntityWithLazyProperty entity;


	@BeforeEach
	public void prepare(SessionFactoryScope scope) throws Exception {
		EntityPersister ep = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(
				EntityWithLazyProperty.class.getName() );
		assertTrue( ep.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() );

		byte[] testArray = new byte[] { 0x2A };

		scope.inTransaction( em -> {
			//persist the test entity.d
			entity = new EntityWithLazyProperty();
			entity.setSomeField( "TEST" );
			entity.setLazyData( testArray );
			em.persist( entity );
		} );

		checkLazyField( scope, entity, testArray );
	}

	/**
	 * Set a non lazy field, therefore the lazyData field will be LazyPropertyInitializer.UNFETCHED_PROPERTY
	 * for both state and newState so the field should not change. This should no longer cause a ClassCastException.
	 */
	@Test
	public void testNoUpdate(SessionFactoryScope scope) {
		byte[] testArray = new byte[] { 0x2A };

		doInJPA( scope::getSessionFactory, new JPATransactionVoidFunction() {
			@Override
			public void accept(EntityManager em) {
				entity = em.find( EntityWithLazyProperty.class, entity.id );
				entity.setSomeField( "TEST1" );
				assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
			}

			@Override
			public void afterTransactionCompletion() {
				assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
			}
		} );

		checkLazyField( scope, entity, testArray );
	}

	/**
	 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
	 * PreUpdate annotated callback method. So state == LazyPropertyInitializer.UNFETCHED_PROPERTY and
	 * newState == EntityWithLazyProperty.PRE_UPDATE_VALUE. This should no longer cause a ClassCastException.
	 */
	@Test
	public void testPreUpdate(SessionFactoryScope scope) {
		doInJPA( scope::getSessionFactory, new JPATransactionVoidFunction() {
			@Override
			public void accept(EntityManager em) {
				entity = em.find( EntityWithLazyProperty.class, entity.id );
				entity.setUpdateLazyFieldInPreUpdate( true );
				entity.setSomeField( "TEST2" );
				assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
			}

			@Override
			public void afterTransactionCompletion() {
				assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
			}
		} );

		checkLazyField( scope, entity, EntityWithLazyProperty.PRE_UPDATE_VALUE );
	}

	/**
	 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
	 * PreUpdate annotated callback method and also set the lazyData field directly to testArray1. When we reload we
	 * should get EntityWithLazyProperty.PRE_UPDATE_VALUE.
	 */
	@Test
	public void testPreUpdateOverride(SessionFactoryScope scope) {
		byte[] testArray = new byte[] { 0x2A };

		scope.inTransaction( em -> {
			entity = em.find( EntityWithLazyProperty.class, entity.id );
			entity.setUpdateLazyFieldInPreUpdate( true );
			assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
			entity.setLazyData( testArray );
			assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
			entity.setSomeField( "TEST3" );
		} );

		checkLazyField( scope, entity, EntityWithLazyProperty.PRE_UPDATE_VALUE );
	}

	private void checkLazyField(SessionFactoryScope scope, EntityWithLazyProperty entity, byte[] expected) {
		// reload the entity and check the lazy value matches what we expect.
		scope.inTransaction( em -> {
			EntityWithLazyProperty testEntity = em.find( EntityWithLazyProperty.class, entity.id );
			assertFalse( Hibernate.isPropertyInitialized( testEntity, "lazyData" ) );
			assertTrue( Arrays.equals( expected, testEntity.lazyData ) );
			assertTrue( Hibernate.isPropertyInitialized( testEntity, "lazyData" ) );
		} );
	}

	// --- //

	/**
	 * Test entity with a lazy property which requires build time instrumentation.
	 *
	 * @author Martin Ball
	 */
	@Entity
	@Table(name = "ENTITY_WITH_LAZY_PROPERTY")
	static class EntityWithLazyProperty {

		public static final byte[] PRE_UPDATE_VALUE = new byte[] { 0x2A, 0x2A, 0x2A, 0x2A };

		@Id
		@GeneratedValue
		private Long id;

		@Basic(fetch = FetchType.LAZY)
		private byte[] lazyData;

		private String someField;

		private boolean updateLazyFieldInPreUpdate;

		public void setLazyData(byte[] lazyData) {
			this.lazyData = lazyData;
		}

		public void setSomeField(String someField) {
			this.someField = someField;
		}

		public void setUpdateLazyFieldInPreUpdate(boolean updateLazyFieldInPreUpdate) {
			this.updateLazyFieldInPreUpdate = updateLazyFieldInPreUpdate;
		}

		@PreUpdate
		public void onPreUpdate() {
			//Allow the update of the lazy field from within the pre update to check that this does not break things.
			if ( updateLazyFieldInPreUpdate ) {
				this.lazyData = PRE_UPDATE_VALUE;
			}
		}
	}
}
