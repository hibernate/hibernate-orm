/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.List;
import jakarta.persistence.PersistenceException;

import org.hibernate.Hibernate;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests merging multiple detached representations of the same entity when explicitly allowed.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ops/Hoarder.hbm.xml"
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "allow"))
public class MergeMultipleEntityCopiesAllowedTest {

	@Test
	public void testNestedDiffBasicProperty(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );

		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// change basic property of nested entity
		item1_1.setName( "item1_1 name" );

		// change the nested Item to be the copy with the new name
		item1.getCategory().setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the name from the top level item will win.
					assertThat( item1Merged.getName(), is( item1.getName() ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item1Get = session.get( Item.class, item1.getId() );
					assertThat( item1Get.getName(), is( item1.getName() ) );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testNestedManyToOneChangedToNull(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );

		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// change many-to-one in nested entity to null.
		item1_1.setCategory( null );
		item1.getCategory().setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the many-to-one from the top level item will win.
					assertThat( item1Merged.getCategory().getName(), is( category.getName() ) );
					assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getCategory().getName(), is( category.getName() ) );
					assertSame( item, item.getCategory().getExampleItem() );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testNestedManyToOneChangedToNewEntity(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );

		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// change many-to-one in nested entity to a new (transient) value
		Category categoryNew = new Category();
		categoryNew.setName( "new category" );
		item1_1.setCategory( categoryNew );
		item1.getCategory().setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the many-to-one from the top level item will win.
					assertThat( item1Merged.getCategory().getName(), is( category.getName() ) );
					assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getCategory().getName(), is( category.getName() ) );
					assertSame( item, item.getCategory().getExampleItem() );
					// make sure new category got persisted
					Category categoryQueried = (Category) session.createQuery(
							"from Category c where c.name='new category'" )
							.uniqueResult();
					assertNotNull( categoryQueried );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testTopLevelManyToOneChangedToNewEntity(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );

		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// change many-to-one in top level to be a new (transient)
		Category categoryNewer = new Category();
		categoryNewer.setName( "newer category" );
		item1.setCategory( categoryNewer );

		// put the other representation in categoryNewer
		categoryNewer.setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the many-to-one from the top level item will win.
					assertThat( item1Merged.getCategory().getName(), is( categoryNewer.getName() ) );
					assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getCategory().getName(), is( categoryNewer.getName() ) );
					assertSame( item, item.getCategory().getExampleItem() );
					// make sure original category is still there
					Category categoryQueried = (Category) session.createQuery( "from Category c where c.name='category'" )
							.uniqueResult();
					assertNotNull( categoryQueried );
					// make sure original category has the same item.
					assertSame( item, categoryQueried.getExampleItem() );
					// set exampleItem to null to avoid constraint violation on cleanup.
					categoryQueried.setExampleItem( null );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testTopLevelManyToOneManagedNestedIsDetached(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );

					item1Merged.setCategory( category );
					category.setExampleItem( item1_1 );

					// now item1Merged is managed and it has a nested detached item
					session.merge( item1Merged );
					assertThat( item1Merged.getCategory().getName(), is( category.getName() ) );
					assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getCategory().getName(), is( category.getName() ) );
					assertSame( item, item.getCategory().getExampleItem() );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testNestedValueCollectionWithChangedElements(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.getColors().add( "red" );

		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					Hibernate.initialize( item.getColors() );
					return item;
				}
		);

		scope.inTransaction(
				session -> {
					// add an element to collection in nested entity
					item1_1.getColors().add( "blue" );
					item1.getCategory().setExampleItem( item1_1 );
				}
		);

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the collection from the top level item will win.
					assertThat( item1Merged.getColors().size(), is( 1 ) );
					assertThat( item1Merged.getColors().iterator().next(), is( "red" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getColors().size(), is( 1 ) );
					assertThat( item.getColors().iterator().next(), is( "red" ) );
					Hibernate.initialize( item.getCategory() );
				}
		);


		// get another representation of item1
		Item item1_2 = scope.fromTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					Hibernate.initialize( item.getColors() );
					return item;
				}
		);


		// remove the existing elements from collection in nested entity
		item1_2.getColors().clear();
		item1.getCategory().setExampleItem( item1_2 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the collection from the top level item will win.
					assertThat( item1Merged.getColors().size(), is( 1 ) );
					assertThat( item1Merged.getColors().iterator().next(), is( "red" ) );
				}
		);


		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getColors().size(), is( 1 ) );
					assertThat( item.getColors().iterator().next(), is( "red" ) );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testTopValueCollectionWithChangedElements(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.getColors().add( "red" );

		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// add an element to collection in nested entity
		item1.getColors().add( "blue" );
		item1.getCategory().setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// the collection from the top level item will win.
					assertThat( item1Merged.getColors().size(), is( 2 ) );
					assertTrue( item1Merged.getColors().contains( "red" ) );
					assertTrue( item1Merged.getColors().contains( "blue" ) );
				}
		);

		Item item1_3 = scope.fromTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getColors().size(), is( 2 ) );
					assertTrue( item.getColors().contains( "red" ) );
					assertTrue( item.getColors().contains( "blue" ) );
					Hibernate.initialize( item.getCategory() );
					return item;
				}
		);

		// get another representation of item1
		Item item1_2 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// remove the existing elements from collection in nested entity
		item1_3.getColors().clear();
		item1_3.getCategory().setExampleItem( item1_2 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1_3 );
					// the collection from the top level item will win.
					assertTrue( item1Merged.getColors().isEmpty() );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1_3.getId() );
					assertTrue( item.getColors().isEmpty() );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testCascadeFromTransientToNonDirtyRepresentations(SessionFactoryScope scope) {

		Item item1 = new Item();
		item1.setName( "item1" );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// Get another representation of the same Item from a different session.

		Item item1_1 = scope.fromSession(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertNotSame( item1, item1_1 );
		assertEquals( item1, item1_1 );

		// Create a transient entity that references both representationsession.
		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		Hoarder mergedHoarder = scope.fromTransaction(
				session -> {
					Hoarder mHoarder = (Hoarder) session.merge( hoarder );
					assertThat( mHoarder.getItems().size(), is( 1 ) );
					assertSame( mHoarder.getFavoriteItem(), mHoarder.getItems().iterator().next() );
					assertThat( mHoarder.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( mHoarder.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
					return mHoarder;
				}
		);

		scope.inTransaction(
				session -> {
					Hoarder h = (Hoarder) session.merge( mergedHoarder );
					assertThat( hoarder.getItems().size(), is( 1 ) );
					assertSame( h.getFavoriteItem(), h.getItems().iterator().next() );
					assertThat( h.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( h.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testCascadeFromDetachedToNonDirtyRepresentations(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1" );

		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );

		scope.inTransaction(
				session -> {
					session.persist( item1 );
					session.persist( hoarder );
				}
		);

		// Get another representation of the same Item from a different session.

		Item item1_1 = scope.fromSession(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertNotSame( item1, item1_1 );
		assertEquals( item1, item1_1 );

		// Update hoarder (detached) to references both representationsession.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		Hoarder mergedHoarder = scope.fromTransaction(
				session -> {
					Hoarder h = (Hoarder) session.merge( hoarder );
					assertThat( h.getItems().size(), is( 1 ) );
					assertSame( h.getFavoriteItem(), h.getItems().iterator().next() );
					assertThat( h.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( h.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
					return h;
				}
		);

		scope.inTransaction(
				session -> {
					Hoarder h = (Hoarder) session.merge( mergedHoarder );
					assertThat( h.getItems().size(), is( 1 ) );
					assertSame( h.getFavoriteItem(), h.getItems().iterator().next() );
					assertThat( h.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( h.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testCascadeFromDetachedToGT2DirtyRepresentations(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1" );
		Category category1 = new Category();
		category1.setName( "category1" );
		item1.setCategory( category1 );

		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );

		scope.inTransaction(
				session -> {
					session.persist( item1 );
					session.persist( hoarder );
				}
		);

		// Get another representation of the same Item from a different session.

		Item item1_1 = scope.fromSession(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// item1 and item1_1 are unmodified representations of the same persistent entity.
		assertNotSame( item1, item1_1 );
		assertEquals( item1, item1_1 );

		// Get another representation of the same Item from a different session.

		Item item1_2 = scope.fromSession(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertNotSame( item1, item1_2 );
		assertEquals( item1, item1_2 );

		item1_1.setName( "item1_1" );
		item1_2.setName( "item1_2" );

		// Update hoarder (detached) to references both representationsession.
		item1.getCategory().setExampleItem( item1_2 );
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );
		hoarder.getFavoriteItem().getCategory();

		Hoarder mergedHoarder = scope.fromTransaction(
				session -> {
					Hoarder mHoarder = (Hoarder) session.merge( hoarder );
					assertThat( mHoarder.getItems().size(), is( 1 ) );
					assertSame( mHoarder.getFavoriteItem(), mHoarder.getItems().iterator().next() );
					assertSame( mHoarder.getFavoriteItem(), mHoarder.getFavoriteItem().getCategory().getExampleItem() );
					assertThat( mHoarder.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( mHoarder.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
					assertThat( mHoarder.getFavoriteItem().getName(), is( item1.getName() ) );
					return mHoarder;
				}
		);

		scope.inTransaction(
				session -> {
					Hoarder h = (Hoarder) session.merge( mergedHoarder );
					assertThat( h.getItems().size(), is( 1 ) );
					assertSame( h.getFavoriteItem(), h.getItems().iterator().next() );
					assertSame( h.getFavoriteItem(), h.getFavoriteItem().getCategory().getExampleItem() );
					assertThat( h.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( h.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testTopLevelEntityNewerThanNested(SessionFactoryScope scope) {
		Item item = new Item();
		item.setName( "item" );

		Category category = new Category();
		category.setName( "category" );

		scope.inTransaction(
				session -> {
					session.persist( item );
					session.persist( category );
				}
		);

		// Get the Category from a different session.
		Category category1_2 = scope.fromTransaction(
				session ->
						session.get( Category.class, category.getId() )
		);

		// Get and update the same Category.
		Category category1_1 = scope.fromTransaction(
				session -> {
					Category category1 = session.get( Category.class, category.getId() );
					category1.setName( "new name" );
					return category1;
				}
		);

		assertTrue( category1_2.getVersion() < category1_1.getVersion() );

		category1_1.setExampleItem( item );
		item.setCategory( category1_2 );

		scope.inTransaction(
				session -> {
					try {
						// representation merged at top level is newer than nested representation.
						session.merge( category1_1 );
						fail( "should have failed because one representation is an older version." );
					}
					catch (PersistenceException e) {
						// expected
						assertTyping( StaleObjectStateException.class, e.getCause() );
					}
				}
		);

		cleanup( scope );
	}

	@Test
	public void testNestedEntityNewerThanTopLevel(SessionFactoryScope scope) {
		Item item = new Item();
		item.setName( "item" );

		Category category = new Category();
		category.setName( "category" );

		scope.inTransaction(
				session -> {
					session.persist( item );
					session.persist( category );
				}
		);

		// Get category1_1 from a different session.
		Category category1_1 = scope.fromSession(
				session ->
						session.get( Category.class, category.getId() )
		);

		// Get and update category1_2 to increment its version.
		Category category1_2 = scope.fromTransaction(
				session -> {
					Category category1 = session.get( Category.class, category.getId() );
					category1.setName( "new name" );
					return category1;
				}
		);

		assertTrue( category1_2.getVersion() > category1_1.getVersion() );

		category1_1.setExampleItem( item );
		item.setCategory( category1_2 );

		scope.inTransaction(
				session -> {
					try {
						// nested representation is newer than top lever representation.
						session.merge( category1_1 );
						fail( "should have failed because one representation is an older version." );
					}
					catch (PersistenceException e) {
						// expected
						assertTyping( StaleObjectStateException.class, e.getCause() );
					}
				}
		);

		cleanup( scope );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9240")
	public void testTopLevelUnidirOneToManyBackrefWithNewElement(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		assertFalse( Hibernate.isInitialized( item1_1.getSubItemsBackref() ) );

		Category category = new Category();
		category.setName( "category" );

		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1.getSubItemsBackref().add( subItem2 );

		item1.setCategory( category );
		category.setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					// The following will fail due to PropertyValueException because item1 will
					// be removed from the inverted merge map when the operation cascades to item1_1.
					Item item1Merged = (Item) session.merge( item1 );
					// top-level collection should win
					assertThat( item1.getSubItemsBackref().size(), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 2 ) );
				}
		);

		cleanup( scope );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9239")
	public void testNestedUnidirOneToManyBackrefWithNewElement(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					Hibernate.initialize( item.getSubItemsBackref() );
					return item;
				}
		);

		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );

		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1_1.getSubItemsBackref().add( subItem2 );

		category.setExampleItem( item1_1 );

		Item item1Merged = scope.fromTransaction(
				session -> {
					Item item = (Item) session.merge( item1 );
					// The resulting collection should contain the added element
					assertThat( item.getSubItemsBackref().size(), is( 2 ) );
					assertThat( item.getSubItemsBackref().get( 0 ).getName(), is( "subItem1 name" ) );
					assertThat( item.getSubItemsBackref().get( 1 ).getName(), is( "subItem2 name" ) );
					return item;
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 2 ) );
					assertThat( item.getSubItemsBackref().get( 0 ).getName(), is( "subItem1 name" ) );
					assertThat( item1Merged.getSubItemsBackref().get( 1 ).getName(), is( "subItem2 name" ) );
				}
		);

		cleanup( scope );
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-9106" )
	public void testTopLevelUnidirOneToManyBackrefWithRemovedElement(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );
		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1.getSubItemsBackref().add( subItem2 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		assertFalse( Hibernate.isInitialized( item1_1.getSubItemsBackref() ) );

		Category category = new Category();
		category.setName( "category" );

		item1.setCategory( category );
		category.setExampleItem( item1_1 );

		// remove subItem1 from top-level Item
		item1.getSubItemsBackref().remove( subItem1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// entity should have been removed
					assertThat( item1Merged.getSubItemsBackref().size(), is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 1 ) );
					SubItem subItem = session.get( SubItem.class, subItem1.getId() );
					// cascade does not include delete-orphan, so subItem1 should still be persistent.
					assertNotNull( subItem );
				}
		);


		cleanup( scope );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9239")
	public void testNestedUnidirOneToManyBackrefWithRemovedElement(SessionFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		SubItem subItem1 = new SubItem();
		subItem1.setName( "subItem1 name" );
		item1.getSubItemsBackref().add( subItem1 );
		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1.getSubItemsBackref().add( subItem2 );

		scope.inTransaction(
				session ->
						session.persist( item1 )
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					Hibernate.initialize( item.getSubItemsBackref() );
					return item;
				}
		);

		// remove subItem1 from the nested Item
		item1_1.getSubItemsBackref().remove( subItem1 );

		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );
		category.setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// entity should have been removed
					assertThat( item1Merged.getSubItemsBackref().size(), is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 1 ) );
					SubItem subItem = session.get( SubItem.class, subItem1.getId() );
					// cascade does not include delete-orphan, so subItem1 should still be persistent.
					assertNotNull( subItem );
				}
		);

		cleanup( scope );
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-9106" )
	public void testTopLevelUnidirOneToManyNoBackrefWithNewElement(SessionFactoryScope scope) {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );

		scope.inTransaction(
				session ->
						session.persist( category1 )
		);

		// get another representation of category1
		Category category1_1 = scope.fromTransaction(
				session ->
						session.get( Category.class, category1.getId() )
		);

		assertFalse( Hibernate.isInitialized( category1_1.getSubCategories() ) );

		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1.getSubCategories().add( subCategory2 );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		scope.inTransaction(
				session -> {
					Category category = (Category) session.merge( category1 );
					assertThat( category.getSubCategories().size(), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Category category = session.get( Category.class, category1.getId() );
					assertThat( category.getSubCategories().size(), is( 2 ) );
				}
		);

		cleanup( scope );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9239")
	public void testNestedUnidirOneToManyNoBackrefWithNewElement(SessionFactoryScope scope) {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );

		scope.inTransaction(
				session ->
						session.persist( category1 )
		);

		// get another representation of category1
		Category category1_1 = scope.fromTransaction(
				session -> {
					Category category = session.get( Category.class, category1.getId() );
					Hibernate.initialize( category.getSubCategories() );
					return category;
				}
		);

		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1_1.getSubCategories().add( subCategory2 );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		scope.inTransaction(
				session -> {
					Category category1Merged = (Category) session.merge( category1 );
					assertThat( category1Merged.getSubCategories().size(), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Category category = session.get( Category.class, category1.getId() );
					assertThat( category.getSubCategories().size(), is( 2 ) );
				}
		);

		cleanup( scope );
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-9106" )
	public void testTopLevelUnidirOneToManyNoBackrefWithRemovedElement(SessionFactoryScope scope) {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );
		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1.getSubCategories().add( subCategory2 );

		scope.inTransaction(
				session ->
						session.persist( category1 )
		);

		// get another representation of category1
		Category category1_1 = scope.fromTransaction(
				session ->
						session.get( Category.class, category1.getId() )
		);

		assertFalse( Hibernate.isInitialized( category1_1.getSubCategories() ) );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		category1.getSubCategories().remove( subCategory1 );

		scope.inTransaction(
				session -> {
					Category category1Merged = (Category) session.merge( category1 );
					assertThat( category1Merged.getSubCategories().size(), is( 1 ) );
					assertTrue( category1Merged.getSubCategories().contains( subCategory2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Category category = session.get( Category.class, category1.getId() );
					assertThat( category.getSubCategories().size(), is( 1 ) );
					assertTrue( category.getSubCategories().contains( subCategory2 ) );
					// cascade does not include delete-orphan, so subCategory1 should still be persistent.
					SubCategory subCategory = session.get( SubCategory.class, subCategory1.getId() );
					assertNotNull( subCategory );
				}
		);

		cleanup( scope );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-9239")
	public void testNestedUnidirOneToManyNoBackrefWithRemovedElement(SessionFactoryScope scope) {
		Category category1 = new Category();
		category1.setName( "category1 name" );
		SubCategory subCategory1 = new SubCategory();
		subCategory1.setName( "subCategory1 name" );
		category1.getSubCategories().add( subCategory1 );
		SubCategory subCategory2 = new SubCategory();
		subCategory2.setName( "subCategory2 name" );
		category1.getSubCategories().add( subCategory2 );

		scope.inTransaction(
				session ->
						session.persist( category1 )
		);

		// get another representation of category1
		Category category1_1 = scope.fromTransaction(
				session -> {
					Category category = session.get( Category.class, category1.getId() );
					Hibernate.initialize( category.getSubCategories() );
					return category;
				}
		);

		category1_1.getSubCategories().remove( subCategory2 );

		Item item = new Item();
		item.setName( "item" );
		category1.setExampleItem( item );
		item.setCategory( category1_1 );

		scope.inTransaction(
				session -> {
					Category category1Merged = (Category) session.merge( category1 );
					assertThat( category1Merged.getSubCategories().size(), is( 1 ) );
					assertTrue( category1Merged.getSubCategories().contains( subCategory2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Category category = session.get( Category.class, category1.getId() );
					assertThat( category.getSubCategories().size(), is( 1 ) );
					assertTrue( category.getSubCategories().contains( subCategory2 ) );
					// cascade does not include delete-orphan, so subCategory1 should still be persistent.
					SubCategory subCategory = session.get( SubCategory.class, subCategory1.getId() );
					assertNotNull( subCategory );
				}
		);

		cleanup( scope );
	}

	@SuppressWarnings("unchecked")
	private void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from SubItem" ).executeUpdate();
					for ( Hoarder hoarder : (List<Hoarder>) session.createQuery( "from Hoarder" ).list() ) {
						hoarder.getItems().clear();
						session.remove( hoarder );
					}

					for ( Category category : (List<Category>) session.createQuery( "from Category" ).list() ) {
						Item exampleItem = category.getExampleItem();
						if ( exampleItem != null ) {
							category.setExampleItem( null );
							exampleItem.setCategory( null );
							session.remove( category );
							session.remove( exampleItem );
						}
					}

					for ( Item item : (List<Item>) session.createQuery( "from Item" ).list() ) {
						Category category = item.getCategory();
						item.setCategory( null );
						if ( category != null ) {
							category.setExampleItem( null );
						}
						session.remove( item );
					}

					session.createQuery( "delete from Item" ).executeUpdate();
				}
		);
	}
}
