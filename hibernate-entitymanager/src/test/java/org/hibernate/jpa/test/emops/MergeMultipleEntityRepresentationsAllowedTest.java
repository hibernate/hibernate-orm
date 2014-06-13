/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.emops;

import java.util.List;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.internal.core.JpaEntityCopyAllowedMergeEventListener;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Tests merging multiple detached representations of the same entity using
 * {@link org.hibernate.jpa.event.internal.core.JpaEntityCopyAllowedMergeEventListener}.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
public class MergeMultipleEntityRepresentationsAllowedTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		super.afterEntityManagerFactoryBuilt();

		SessionFactoryImplementor sfi = entityManagerFactory().unwrap( SessionFactoryImplementor.class );
		EventListenerRegistry registry = sfi.getServiceRegistry().getService( EventListenerRegistry.class );
		registry.setListeners( EventType.MERGE, new JpaEntityCopyAllowedMergeEventListener() );
	}

	@Test
	public void testCascadeFromDetachedToNonDirtyRepresentations() {
		Item item1 = new Item();
		item1.setName( "item1" );

		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item1 );
		em.persist( hoarder );
		em.getTransaction().commit();
		em.close();

		// Get another representation of the same Item from a different EntityManager.

		em = getOrCreateEntityManager();
		Item item1_1 = em.find( Item.class, item1.getId() );
		em.close();

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_1 );
		assertTrue( item1.equals( item1_1 ) );

		// Update hoarder (detached) to references both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		hoarder = em.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		hoarder = em.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		em.getTransaction().commit();
		em.close();

		cleanup();
	}


	@Test
	public void testTopLevelManyToOneManagedNestedIsDetached() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );
		category.setExampleItem( item1 );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item1 );
		em.getTransaction().commit();
		em.close();

		// get another representation of item1
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item item1_1 = em.find( Item.class, item1.getId() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item item1Merged = em.merge( item1 );

		item1Merged.setCategory( category );
		category.setExampleItem( item1_1 );

		// now item1Merged is managed and it has a nested detached item
		em.merge( item1Merged );
		assertEquals( category.getName(), item1Merged.getCategory().getName() );
		assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		item1 = em.find( Item.class, item1.getId() );
		assertEquals( category.getName(), item1.getCategory().getName() );
		assertSame( item1, item1.getCategory().getExampleItem() );
		em.getTransaction().commit();
		em.close();

		cleanup();
	}

	@SuppressWarnings( {"unchecked"})
	private void cleanup() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		for ( Hoarder hoarder : (List<Hoarder>) em.createQuery( "from Hoarder" ).getResultList() ) {
			hoarder.getItems().clear();
			em.remove( hoarder );
		}

		for ( Category category : (List<Category>) em.createQuery( "from Category" ).getResultList() ) {
			if ( category.getExampleItem() != null ) {
				category.setExampleItem( null );
				em.remove( category );
			}
		}

		for ( Item item : (List<Item>) em.createQuery( "from Item" ).getResultList() ) {
			item.setCategory( null );
			em.remove( item );
		}

		em.createQuery( "delete from Item" ).executeUpdate();

		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Category.class,
				Hoarder.class,
				Item.class
		};
	}
}
