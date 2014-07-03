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
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity using a custom EntityCopyObserver.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
public class MergeMultipleEntityCopiesCustomTest extends BaseEntityManagerFunctionalTestCase {

	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put(
				"hibernate.event.merge.entity_copy_observer",
				CustomEntityCopyObserver.class.getName()
		);
	}

	@Test
	public void testMergeMultipleEntityCopiesAllowed() {
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
		// the merge should succeed because it does not have Category copies.
		// (CustomEntityCopyObserver does not allow Category copies; it does allow Item copies)
		hoarder = em.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		hoarder = em.find( Hoarder.class, hoarder.getId() );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		em.getTransaction().commit();
		em.close();

		cleanup();
	}

	@Test
	public void testMergeMultipleEntityCopiesAllowedAndDisallowed() {
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
		// make sure item1_1.category is initialized
		Hibernate.initialize( item1_1.getCategory() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item item1Merged = em.merge( item1 );

		// make sure item1Merged.category is also managed
		Hibernate.initialize( item1Merged.getCategory() );

		item1Merged.setCategory( category );
		category.setExampleItem( item1_1 );

		// now item1Merged is managed and it has a nested detached item
		// and there is  multiple managed/detached Category objects
		try {
			// the following should fail because multiple copies of Category objects is not allowed by
			// CustomEntityCopyObserver
			em.merge( item1Merged );
			fail( "should have failed because CustomEntityCopyObserver does not allow multiple copies of a Category. ");
		}
		catch (IllegalStateException ex ) {
			// expected
		}
		finally {
			em.getTransaction().rollback();
		}
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

	public static class CustomEntityCopyObserver implements EntityCopyObserver {

		@Override
		public void entityCopyDetected(Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource session) {
			if ( Category.class.isInstance( managedEntity ) ) {
				throw new IllegalStateException(
						String.format( "Entity copies of type [%s] not allowed", Category.class.getName() )
				);
			}
		}

		@Override
		public void topLevelMergeComplete(EventSource session) {

		}

		@Override
		public void clear() {

		}
	}
}
