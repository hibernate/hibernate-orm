/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.emops;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity when
 * not allowed by default.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
public class MergeMultipleEntityCopiesDisallowedByDefaultTest extends BaseEntityManagerFunctionalTestCase {

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

		// Get another representation of the same Item from a different session.

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
		try {
			em.merge( hoarder );
			fail( "should have failed due IllegalStateException");
		}
		catch (IllegalStateException ex) {
			//expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
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
		try {
			em.merge( item1Merged );
			fail( "should have failed due IllegalStateException");
		}
		catch (IllegalStateException ex) {
			//expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}

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
