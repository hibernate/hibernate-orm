/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement.cases;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.enhancement.cases.domain.EntityWithLazyProperty;
import org.hibernate.persister.entity.EntityPersister;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestLazyPropertyOnPreUpdateExecutable extends AbstractExecutable {
	@Override
	protected void prepared() {
		final EntityPersister ep = getEntityManagerFactory().getSessionFactory().getEntityPersister( EntityWithLazyProperty.class.getName() );
		assertTrue( ep.getInstrumentationMetadata().isEnhancedForLazyLoading() );
	}

	@Override
	public void execute() {
		EntityWithLazyProperty entity;
		EntityManager em = getOrCreateEntityManager();

		byte[] testArray = new byte[]{0x2A};

		//persist the test entity.
		em.getTransaction().begin();
		entity = new EntityWithLazyProperty();
		entity.setSomeField("TEST");
		entity.setLazyData(testArray);
		em.persist(entity);
		em.getTransaction().commit();
		em.close();

		checkLazyField(entity, em, testArray);

		/**
		 * Set a non lazy field, therefore the lazyData field will be LazyPropertyInitializer.UNFETCHED_PROPERTY
		 * for both state and newState so the field should not change. This should no longer cause a ClassCastException.
		 */
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		entity.setSomeField("TEST1");
		assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.getTransaction().commit();
		assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.close();

		checkLazyField(entity, em, testArray);

		/**
		 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
		 * PreUpdate annotated callback method. So state == LazyPropertyInitializer.UNFETCHED_PROPERTY and
		 * newState == EntityWithLazyProperty.PRE_UPDATE_VALUE. This should no longer cause a ClassCastException.
		 */
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		entity.setUpdateLazyFieldInPreUpdate(true);
		entity.setSomeField("TEST2");
		assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.getTransaction().commit();
		assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.close();

		checkLazyField(entity, em, EntityWithLazyProperty.PRE_UPDATE_VALUE);

		/**
		 * Set the updateLazyFieldInPreUpdate flag so that the lazy field is updated from within the
		 * PreUpdate annotated callback method and also set the lazyData field directly to testArray1. When we reload we
		 * should get EntityWithLazyProperty.PRE_UPDATE_VALUE.
		 */
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		entity.setUpdateLazyFieldInPreUpdate(true);
		assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		entity.setLazyData(testArray);
		assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		entity.setSomeField("TEST3");
		em.getTransaction().commit();
		em.close();

		checkLazyField( entity, em, EntityWithLazyProperty.PRE_UPDATE_VALUE);
	}

	private void checkLazyField(EntityWithLazyProperty entity, EntityManager em, byte[] expected) {
		// reload the entity and check the lazy value matches what we expect.
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		entity = em.find(EntityWithLazyProperty.class, entity.getId());
		assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData") );
		assertTrue( Arrays.equals( expected, entity.getLazyData() ) );
		assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		em.getTransaction().commit();
		em.close();
	}


	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { EntityWithLazyProperty.class };
	}

}
