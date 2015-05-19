/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

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
public class MergeMultipleEntityCopiesCustomTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] {
				"ops/Hoarder.hbm.xml"
		};
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
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

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( item1 );
		s.persist( hoarder );
		s.getTransaction().commit();
		s.close();

		// Get another representation of the same Item.

		s = openSession();
		Item item1_1 = (Item) s.get( Item.class, item1.getId() );
		s.close();

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_1 );
		assertTrue( item1.equals( item1_1 ) );

		// Update hoarder (detached) to references both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		s = openSession();
		s.getTransaction().begin();
		// the merge should succeed because it does not have Category copies.
		// (CustomEntityCopyObserver does not allow Category copies; it does allow Item copies)
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		hoarder = (Hoarder) s.get( Hoarder.class, hoarder.getId() );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		s.getTransaction().commit();
		s.close();

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

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( item1 );
		s.getTransaction().commit();
		s.close();

		// get another representation of item1
		s = openSession();
		s.getTransaction().begin();
		Item item1_1 = (Item) s.get( Item.class, item1.getId() );
		// make sure item1_1.category is initialized
		Hibernate.initialize( item1_1.getCategory() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		Item item1Merged = (Item) s.merge( item1 );

		item1Merged.setCategory( category );
		category.setExampleItem( item1_1 );

		// now item1Merged is managed and it has a nested detached item
		// and there is  multiple managed/detached Category objects
		try {
			// the following should fail because multiple copies of Category objects is not allowed by
			// CustomEntityCopyObserver
			s.merge( item1Merged );
			fail( "should have failed because CustomEntityCopyObserver does not allow multiple copies of a Category. ");
		}
		catch (IllegalStateException ex ) {
			// expected
		}
		finally {
			s.getTransaction().rollback();
		}
		s.close();

		s = openSession();
		s.getTransaction().begin();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( category.getName(), item1.getCategory().getName() );
		assertSame( item1, item1.getCategory().getExampleItem() );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@SuppressWarnings( {"unchecked"})
	private void cleanup() {
		Session s = openSession();
		s.getTransaction().begin();

		for ( Hoarder hoarder : (List<Hoarder>) s.createQuery( "from Hoarder" ).list() ) {
			hoarder.getItems().clear();
			s.delete( hoarder );
		}

		for ( Category category : (List<Category>) s.createQuery( "from Category" ).list() ) {
			if ( category.getExampleItem() != null ) {
				category.setExampleItem( null );
				s.delete( category );
			}
		}

		for ( Item item : (List<Item>) s.createQuery( "from Item" ).list() ) {
			item.setCategory( null );
			s.delete( item );
		}

		s.createQuery( "delete from Item" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
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
