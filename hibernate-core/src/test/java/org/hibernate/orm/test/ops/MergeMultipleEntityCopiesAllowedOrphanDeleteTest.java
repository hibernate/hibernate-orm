/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.List;

import org.hibernate.Hibernate;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity (allowed)
 * where some associations include cascade="delete-orphan"
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ops/HoarderOrphanDelete.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "allow")
)
public class MergeMultipleEntityCopiesAllowedOrphanDeleteTest {

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
					// The following will fail due to PropertyValueException because item1  will
					// be removed from the inverted merge map when the operation cascades to item1_1.
					Item item1Merged = (Item) session.merge( item1 );
					// top-level collection should win
					assertThat( item1Merged.getSubItemsBackref().size(), is( 2 ) );
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

		// Add a new SubItem to the Item representation that will be in a nested association.
		SubItem subItem2 = new SubItem();
		subItem2.setName( "subItem2 name" );
		item1_1.getSubItemsBackref().add( subItem2 );

		category.setExampleItem( item1_1 );

		scope.inTransaction(
				session -> {
					Item item1Merged = (Item) session.merge( item1 );
					// The resulting collection should contain the added element
					assertThat( item1Merged.getSubItemsBackref().size(), is( 2 ) );
					assertThat( item1Merged.getSubItemsBackref().get( 0 ).getName(), is( "subItem1 name" ) );
					assertThat( item1Merged.getSubItemsBackref().get( 1 ).getName(), is( "subItem2 name" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 2 ) );
					assertThat( item.getSubItemsBackref().get( 0 ).getName(), is( "subItem1 name" ) );
					assertThat( item.getSubItemsBackref().get( 1 ).getName(), is( "subItem2 name" ) );
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
					// element should be removed
					assertThat( item1Merged.getSubItemsBackref().size(), is( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 1 ) );
					// because cascade includes "delete-orphan" the removed SubItem should have been deleted.
					SubItem subItem = session.get( SubItem.class, subItem1.getId() );
					assertNull( subItem );
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
					// the element should have been removed
					assertThat( item1Merged.getSubItemsBackref().size(), is( 1 ) );
					assertTrue( item1Merged.getSubItemsBackref().contains( subItem2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					assertThat( item.getSubItemsBackref().size(), is( 1 ) );
					assertTrue( item.getSubItemsBackref().contains( subItem2 ) );
					// because cascade includes "delete-orphan" the removed SubItem should have been deleted.
					SubItem subItem = session.get( SubItem.class, subItem1.getId() );
					assertNull( subItem );
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
					// new element should be there
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
					SubCategory subCategory = session.get( SubCategory.class, subCategory1.getId() );
					assertNull( subCategory );
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
					assertTrue( category1.getSubCategories().contains( subCategory2 ) );
					SubCategory subCategory = session.get( SubCategory.class, subCategory1.getId() );
					assertNull( subCategory );
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
						if ( category.getExampleItem() != null ) {
							category.setExampleItem( null );
							session.remove( category );
						}
					}

					for ( Item item : (List<Item>) session.createQuery( "from Item" ).list() ) {
						item.setCategory( null );
						session.remove( item );
					}

					session.createQuery( "delete from Item" ).executeUpdate();
				}
		);
	}
}
