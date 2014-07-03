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

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity (allowed)
 * where some associations include cascade="delete-orphan"
 *
 * @author Gail Badner
 */
public class MergeMultipleEntityCopiesAllowedOrphanDeleteTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] {
				"ops/HoarderOrphanDelete.hbm.xml"
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
		// The following will fail due to PropertyValueException because item1  will
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

		// Add a new SubItem to the Item representation that will be in a nested association.
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
		assertEquals( "subItem2 name", item1.getSubItemsBackref().get( 1 ).getName() );
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
		// element should be removed
		assertEquals( 1, item1Merged.getSubItemsBackref().size() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 1, item1.getSubItemsBackref().size() );
		// because cascade includes "delete-orphan" the removed SubItem should have been deleted.
		subItem1 = (SubItem) s.get( SubItem.class, subItem1.getId() );
		assertNull( subItem1 );
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
		// the element should have been removed
		assertEquals( 1, item1Merged.getSubItemsBackref().size() );
		assertTrue( item1Merged.getSubItemsBackref().contains( subItem2 ) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		item1 = (Item) s.get( Item.class, item1.getId() );
		assertEquals( 1, item1.getSubItemsBackref().size() );
		assertTrue( item1.getSubItemsBackref().contains( subItem2 ) );
		// because cascade includes "delete-orphan" the removed SubItem should have been deleted.
		subItem1 = (SubItem) s.get( SubItem.class, subItem1.getId() );
		assertNull( subItem1 );
		tx.commit();
		s.close();

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
		// new element should be there
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
		subCategory1 = (SubCategory) s.get( SubCategory.class, subCategory1.getId() );
		assertNull( subCategory1 );
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
		subCategory1 = (SubCategory) s.get( SubCategory.class, subCategory1.getId() );
		assertNull( subCategory1 );
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
