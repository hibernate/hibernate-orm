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
package org.hibernate.test.ops;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.event.internal.EntityCopyAllowedMergeEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity using
 * a the default MergeEventListener (that does not allow this).
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
public class MergeMultipleEntityRepresentationsNotAllowedTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] {
				"ops/Hoarder.hbm.xml"
		};
	}

	@Test
	public void testCascadeFromDetachedToNonDirtyRepresentations() {
		Item item1 = new Item();
		item1.setName( "item1" );

		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );

		Session s = openSession();
		Transaction tx = session.beginTransaction();
		s.persist( item1 );
		s.persist( hoarder );
		tx.commit();
		s.close();

		// Get another representation of the same Item from a different session.

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
		tx = s.beginTransaction();
		try {
			hoarder = (Hoarder) s.merge( hoarder );
			fail( "should have failed due IllegalStateException");
		}
		catch (IllegalStateException ex) {
			//expected
		}
		finally {
			tx.rollback();
			s.close();
		}

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

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( item1 );
		tx.commit();
		s.close();

		// get another representation of item1
		s = openSession();
		tx = s.beginTransaction();
		Item item1_1 = (Item) s.get( Item.class, item1.getId() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );

		item1Merged.setCategory( category );
		category.setExampleItem( item1_1 );

		// now item1Merged is managed and it has a nested detached item
		try {
			s.merge( item1Merged );
			fail( "should have failed due IllegalStateException");
		}
		catch (IllegalStateException ex) {
			//expected
		}
		finally {
			tx.rollback();
			s.close();
		}

		cleanup();
	}

	@SuppressWarnings( {"unchecked"})
	private void cleanup() {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "delete from SubItem" ).executeUpdate();
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
}
