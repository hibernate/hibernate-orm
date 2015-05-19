/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests merging multiple detached representations of the same entity when explicitly allowed.
 *
 * @author Gail Badner
 */
public class MergeMultipleEntityCopiesAllowedTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] {
				"ops/Hoarder.hbm.xml"
		};
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				"hibernate.event.merge.entity_copy_observer",
				"allow"
		);
	}

	@Test
	public void testNestedDiffBasicProperty() {
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

		// change basic property of nested entity
		item1_1.setName( "item1_1 name" );

		// change the nested Item to be the copy with the new name
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// the name from the top level item will win.
		assertEquals( item1.getName(), item1Merged.getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Item item1Get = (Item) s.get( Item.class, item1.getId() );
		assertEquals( item1.getName(), item1Get.getName() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testNestedManyToOneChangedToNull() {
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

		// change many-to-one in nested entity to null.
		item1_1.setCategory( null );
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// the many-to-one from the top level item will win.
		assertEquals( category.getName(), item1Merged.getCategory().getName() );
		assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( category.getName(), item1.getCategory().getName() );
		assertSame( item1, item1.getCategory().getExampleItem() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testNestedManyToOneChangedToNewEntity() {
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

		// change many-to-one in nested entity to a new (transient) value
		Category categoryNew = new Category();
		categoryNew.setName( "new category" );
		item1_1.setCategory( categoryNew );
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// the many-to-one from the top level item will win.
		assertEquals( category.getName(), item1Merged.getCategory().getName() );
		assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( category.getName(), item1.getCategory().getName() );
		assertSame( item1, item1.getCategory().getExampleItem() );
		// make sure new category got persisted
		Category categoryQueried = (Category) s.createQuery( "from Category c where c.name='new category'" ).uniqueResult();
		assertNotNull( categoryQueried );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testTopLevelManyToOneChangedToNewEntity() {
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

		// change many-to-one in top level to be a new (transient)
		Category categoryNewer = new Category();
		categoryNewer.setName( "newer category" );
		item1.setCategory( categoryNewer );

		// put the other representation in categoryNewer
		categoryNewer.setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// the many-to-one from the top level item will win.
		assertEquals( categoryNewer.getName(), item1Merged.getCategory().getName() );
		assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( categoryNewer.getName(), item1.getCategory().getName() );
		assertSame( item1, item1.getCategory().getExampleItem() );
		// make sure original category is still there
		Category categoryQueried = (Category) s.createQuery( "from Category c where c.name='category'" ).uniqueResult();
		assertNotNull( categoryQueried );
		// make sure original category has the same item.
		assertSame( item1, categoryQueried.getExampleItem() );
		// set exampleItem to null to avoid constraint violation on cleanup.
		categoryQueried.setExampleItem( null );
		tx.commit();
		s.close();

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
		s.merge( item1Merged );
		assertEquals( category.getName(), item1Merged.getCategory().getName() );
		assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( category.getName(), item1.getCategory().getName() );
		assertSame( item1, item1.getCategory().getExampleItem() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testNestedValueCollectionWithChangedElements() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.getColors().add( "red" );

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
		Hibernate.initialize( item1_1.getColors() );
		tx.commit();
		s.close();

		// add an element to collection in nested entity
		item1_1.getColors().add( "blue" );
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// the collection from the top level item will win.
		assertEquals( 1, item1Merged.getColors().size() );
		assertEquals( "red", item1Merged.getColors().iterator().next() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 1, item1.getColors().size() );
		assertEquals( "red", item1.getColors().iterator().next() );
		Hibernate.initialize( item1.getCategory() );
		tx.commit();
		s.close();

		// get another representation of item1
		s = openSession();
		tx = s.beginTransaction();
		item1_1 = (Item) s.get( Item.class, item1.getId() );
		Hibernate.initialize( item1_1.getColors() );
		tx.commit();
		s.close();

		// remove the existing elements from collection in nested entity
		item1_1.getColors().clear();
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		item1Merged = (Item) s.merge( item1 );
		// the collection from the top level item will win.
		assertEquals( 1, item1Merged.getColors().size() );
		assertEquals( "red", item1Merged.getColors().iterator().next() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 1, item1.getColors().size() );
		assertEquals( "red", item1.getColors().iterator().next() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testTopValueCollectionWithChangedElements() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.getColors().add( "red" );

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

		// add an element to collection in nested entity
		item1.getColors().add( "blue" );
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// the collection from the top level item will win.
		assertEquals( 2, item1Merged.getColors().size() );
		assertTrue( item1Merged.getColors().contains( "red" ) );
		assertTrue( item1Merged.getColors().contains( "blue" ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 2, item1.getColors().size() );
		assertTrue( item1.getColors().contains( "red" ) );
		assertTrue( item1.getColors().contains( "blue" ) );
		Hibernate.initialize( item1.getCategory() );
		tx.commit();
		s.close();

		// get another representation of item1
		s = openSession();
		tx = s.beginTransaction();
		item1_1 = (Item) s.get( Item.class, item1.getId() );
		tx.commit();
		s.close();

		// remove the existing elements from collection in nested entity
		item1.getColors().clear();
		item1.getCategory().setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		item1Merged = (Item) s.merge( item1 );
		// the collection from the top level item will win.
		assertTrue( item1Merged.getColors().isEmpty() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertTrue( item1.getColors().isEmpty() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testCascadeFromTransientToNonDirtyRepresentations() {

		Item item1 = new Item();
		item1.setName( "item1" );

		Session s = openSession();
		Transaction tx = session.beginTransaction();
		s.persist( item1 );
		tx.commit();
		s.close();

		// Get another representation of the same Item from a different session.

		s = openSession();
		Item item1_1 = (Item) s.get( Item.class, item1.getId() );
		s.close();

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_1 );
		assertTrue( item1.equals( item1_1 ) );

		// Create a transient entity that references both representations.
		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		tx.commit();
		s.close();

		cleanup();
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
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testCascadeFromDetachedToGT2DirtyRepresentations() {
		Item item1 = new Item();
		item1.setName( "item1" );
		Category category1 = new Category();
		category1.setName( "category1" );
		item1.setCategory( category1 );

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

		// item1 and item1_1 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_1 );
		assertTrue( item1.equals( item1_1 ) );

		// Get another representation of the same Item from a different session.

		s = openSession();
		Item item1_2 = (Item) s.get( Item.class, item1.getId() );
		s.close();

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_2 );
		assertTrue( item1.equals( item1_2) );

		item1_1.setName( "item1_1" );
		item1_2.setName( "item1_2" );

		// Update hoarder (detached) to references both representations.
		item1.getCategory().setExampleItem( item1_2 );
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );
		hoarder.getFavoriteItem().getCategory();

		s = openSession();
		tx = s.beginTransaction();
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getFavoriteItem().getCategory().getExampleItem() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		assertEquals( item1.getName(), hoarder.getFavoriteItem().getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		hoarder = (Hoarder) s.merge( hoarder );
		assertEquals( 1, hoarder.getItems().size() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getItems().iterator().next() );
		assertSame( hoarder.getFavoriteItem(), hoarder.getFavoriteItem().getCategory().getExampleItem() );
		assertEquals( item1.getId(), hoarder.getFavoriteItem().getId() );
		assertEquals( item1.getCategory(), hoarder.getFavoriteItem().getCategory() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testTopLevelEntityNewerThanNested() {
		Item item = new Item();
		item.setName( "item" );

		Category category = new Category();
		category.setName( "category" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( item );
		s.persist( category );
		tx.commit();
		s.close();

		// Get the Category from a different session.
		s = openSession();
		tx = s.beginTransaction();
		Category category1_2 = (Category) s.get( Category.class, category.getId() );
		tx.commit();
		s.close();

		// Get and update the same Category.
		s = openSession();
		tx = s.beginTransaction();
		Category category1_1 = (Category) s.get( Category.class, category.getId() );
		category1_1.setName( "new name" );
		tx.commit();
		s.close();

		assertTrue( category1_2.getVersion() < category1_1.getVersion() );

		category1_1.setExampleItem( item );
		item.setCategory( category1_2 );

		s = openSession();
		tx = s.beginTransaction();
		try {
			// representation merged at top level is newer than nested representation.
			category1_1 = (Category) s.merge( category1_1 );
			fail( "should have failed because one representation is an older version." );
		}
		catch( StaleObjectStateException ex ) {
			// expected
		}
		finally {
			tx.rollback();
			s.close();
		}

		cleanup();
	}

	@Test
	public void testNestedEntityNewerThanTopLevel() {
		Item item = new Item();
		item.setName( "item" );

		Category category = new Category();
		category.setName( "category" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( item );
		s.persist( category );
		tx.commit();
		s.close();

		// Get category1_1 from a different session.
		s = openSession();
		Category category1_1 = (Category) s.get( Category.class, category.getId() );
		s.close();

		// Get and update category1_2 to increment its version.
		s = openSession();
		tx = s.beginTransaction();
		Category category1_2 = (Category) s.get( Category.class, category.getId() );
		category1_2.setName( "new name" );
		tx.commit();
		s.close();

		assertTrue( category1_2.getVersion() > category1_1.getVersion() );

		category1_1.setExampleItem( item );
		item.setCategory( category1_2 );

		s = openSession();
		tx = s.beginTransaction();
		try {
			// nested representation is newer than top lever representation.
			category1_1 = (Category) s.merge( category1_1 );
			fail( "should have failed because one representation is an older version." );
		}
		catch( StaleObjectStateException ex ) {
			// expected
		}
		finally {
			tx.rollback();
			s.close();
		}

		cleanup();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9240" )
	public void testTopLevelUnidirOneToManyBackrefWithNewElement() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );

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

		assertFalse( Hibernate.isInitialized( item1_1.getSubItemsBackref() ) );

		Category category = new Category();
		category.setName( "category" );

		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1.getSubItemsBackref().add( subItem2 );

		item1.setCategory( category );
		category.setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		// The following will fail due to PropertyValueException because item1 will
		// be removed from the inverted merge map when the operation cascades to item1_1.
		Item item1Merged = (Item) s.merge( item1 );
		// top-level collection should win
		assertEquals( 2, item1.getSubItemsBackref().size() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 2, item1.getSubItemsBackref().size() );
		tx.commit();
		s.close();


		cleanup();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9239" )
	public void testNestedUnidirOneToManyBackrefWithNewElement() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( item1 );
		tx.commit();
		s.close();

		// get another representation of item1
		s = openSession();
		tx = s.beginTransaction();
		Item item1_1 = (Item) s.get( Item.class, item1.getId() );
		Hibernate.initialize( item1_1.getSubItemsBackref() );
		tx.commit();
		s.close();

		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );

		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1_1.getSubItemsBackref().add( subItem2 );

		category.setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// The resulting collection should contain the added element
		assertEquals( 2, item1Merged.getSubItemsBackref().size() );
		assertEquals( "subItem1 name", item1Merged.getSubItemsBackref().get( 0 ).getName() );
		assertEquals( "subItem2 name", item1Merged.getSubItemsBackref().get( 1 ).getName() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 2, item1.getSubItemsBackref().size() );
		assertEquals( "subItem1 name", item1.getSubItemsBackref().get( 0 ).getName() );
		assertEquals( "subItem2 name", item1Merged.getSubItemsBackref().get( 1 ).getName() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-9106" )
	public void testTopLevelUnidirOneToManyBackrefWithRemovedElement() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );
		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1.getSubItemsBackref().add( subItem2 );

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

		assertFalse( Hibernate.isInitialized( item1_1.getSubItemsBackref() ) );

		Category category = new Category();
		category.setName( "category" );

		item1.setCategory( category );
		category.setExampleItem( item1_1 );

		// remove subItem1 from top-level Item
		item1.getSubItemsBackref().remove( subItem1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// entity should have been removed
		assertEquals( 1, item1Merged.getSubItemsBackref().size() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 1, item1.getSubItemsBackref().size() );
		subItem1 = (SubItem) s.get( SubItem.class, subItem1.getId() );
		// cascade does not include delete-orphan, so subItem1 should still be persistent.
		assertNotNull( subItem1 );
		tx.commit();


		cleanup();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9239" )
	public void testNestedUnidirOneToManyBackrefWithRemovedElement() {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );
		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1.getSubItemsBackref().add( subItem2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( item1 );
		tx.commit();
		s.close();

		// get another representation of item1
		s = openSession();
		tx = s.beginTransaction();
		Item item1_1 = (Item) s.get( Item.class, item1.getId() );
		Hibernate.initialize( item1_1.getSubItemsBackref() );
		tx.commit();
		s.close();

		// remove subItem1 from the nested Item
		item1_1.getSubItemsBackref().remove( subItem1 );

		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );
		category.setExampleItem( item1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Item item1Merged = (Item) s.merge( item1 );
		// entity should have been removed
		assertEquals( 1, item1Merged.getSubItemsBackref().size() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 1, item1.getSubItemsBackref().size() );
		subItem1 = (SubItem) s.get( SubItem.class, subItem1.getId() );
		// cascade does not include delete-orphan, so subItem1 should still be persistent.
		assertNotNull( subItem1 );
		tx.commit();

		cleanup();
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-9106" )
	public void testTopLevelUnidirOneToManyNoBackrefWithNewElement() {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( category1 );
		tx.commit();
		s.close();

		// get another representation of category1
		s = openSession();
		tx = s.beginTransaction();
		Category category1_1 = (Category) s.get( Category.class, category1.getId() );
		tx.commit();
		s.close();

		assertFalse( Hibernate.isInitialized( category1_1.getSubCategories() ) );

		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1.getSubCategories().add( subCategory2 );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Category category1Merged = (Category) s.merge( category1 );
		assertEquals( 2, category1Merged.getSubCategories().size() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		category1 = (Category) s.get( Category.class, category1.getId() );
		assertEquals( 2, category1.getSubCategories().size() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9239" )
	public void testNestedUnidirOneToManyNoBackrefWithNewElement() {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( category1 );
		tx.commit();
		s.close();

		// get another representation of category1
		s = openSession();
		tx = s.beginTransaction();
		Category category1_1 = (Category) s.get( Category.class, category1.getId() );
		Hibernate.initialize( category1_1.getSubCategories() );
		tx.commit();
		s.close();

		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1_1.getSubCategories().add( subCategory2 );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Category category1Merged = (Category) s.merge( category1 );
		assertEquals( 2, category1Merged.getSubCategories().size() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		category1 = (Category) s.get( Category.class, category1.getId() );
		assertEquals( 2, category1.getSubCategories().size() );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-9106" )
	public void testTopLevelUnidirOneToManyNoBackrefWithRemovedElement() {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );
		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1.getSubCategories().add( subCategory2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( category1 );
		tx.commit();
		s.close();

		// get another representation of category1
		s = openSession();
		tx = s.beginTransaction();
		Category category1_1 = (Category) s.get( Category.class, category1.getId() );
		tx.commit();
		s.close();

		assertFalse( Hibernate.isInitialized( category1_1.getSubCategories() ) );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		category1.getSubCategories().remove( subCategory1 );

		s = openSession();
		tx = s.beginTransaction();
		Category category1Merged = (Category) s.merge( category1 );
		assertEquals( 1, category1Merged.getSubCategories().size() );
		assertTrue( category1Merged.getSubCategories().contains( subCategory2 ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		category1 = (Category) s.get( Category.class, category1.getId() );
		assertEquals( 1, category1.getSubCategories().size() );
		assertTrue( category1.getSubCategories().contains( subCategory2 ) );
		// cascade does not include delete-orphan, so subCategory1 should still be persistent.
		subCategory1 = (SubCategory) s.get( SubCategory.class, subCategory1.getId() );
		assertNotNull( subCategory1 );
		tx.commit();
		s.close();

		cleanup();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-9239" )
	public void testNestedUnidirOneToManyNoBackrefWithRemovedElement() {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );
		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1.getSubCategories().add( subCategory2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( category1 );
		tx.commit();
		s.close();

		// get another representation of category1
		s = openSession();
		tx = s.beginTransaction();
		Category category1_1 = (Category) s.get( Category.class, category1.getId() );
		Hibernate.initialize( category1_1.getSubCategories() );
		tx.commit();
		s.close();

		category1_1.getSubCategories().remove( subCategory2 );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		s = openSession();
		tx = s.beginTransaction();
		Category category1Merged = (Category) s.merge( category1 );
		assertEquals( 1, category1Merged.getSubCategories().size() );
		assertTrue( category1Merged.getSubCategories().contains( subCategory2 ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		category1 = (Category) s.get( Category.class, category1.getId() );
		assertEquals( 1, category1.getSubCategories().size() );
		assertTrue( category1.getSubCategories().contains( subCategory2 ) );
		// cascade does not include delete-orphan, so subCategory1 should still be persistent.
		subCategory1 = (SubCategory) s.get( SubCategory.class, subCategory1.getId() );
		assertNotNull( subCategory1 );
		tx.commit();
		s.close();

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
			Item exampleItem = category.getExampleItem();
			if ( exampleItem != null ) {
				category.setExampleItem( null );
				exampleItem.setCategory( null );
				s.delete( category );
				s.delete (exampleItem );
			}
		}

		for ( Item item : (List<Item>) s.createQuery( "from Item" ).list() ) {
			Category category = item.getCategory();
			item.setCategory( null );
			if ( category != null ) {
				category.setExampleItem( null );
			}
			s.delete( item );
		}

		s.createQuery( "delete from Item" ).executeUpdate();


		s.getTransaction().commit();
		s.close();
	}
}
