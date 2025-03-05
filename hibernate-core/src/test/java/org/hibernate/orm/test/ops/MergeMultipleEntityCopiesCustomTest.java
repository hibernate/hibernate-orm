/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests merging multiple detached representations of the same entity using a custom EntityCopyObserver.
 *
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9106")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ops/Hoarder.hbm.xml",
		annotatedClasses = {
				Category.class,
				Hoarder.class,
				Item.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "org.hibernate.orm.test.ops.CustomEntityCopyObserver")
)
public class MergeMultipleEntityCopiesCustomTest {

	@Test
	public void testMergeMultipleEntityCopiesAllowed(SessionFactoryScope scope) {
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

		// Get another representation of the same Item.

		Item item1_1 = scope.fromTransaction(
				session ->
						session.get( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertNotSame( item1, item1_1 );
		assertEquals( item1, item1_1 );

		// Update hoarder (detached) to references both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		Hoarder h = scope.fromTransaction(
				session -> {
					// the merge should succeed because it does not have Category copies.
					// (CustomEntityCopyObserver does not allow Category copies; it does allow Item copies)
					Hoarder h1 = (Hoarder) session.merge( hoarder );
					assertThat( h1.getItems().size(), is( 1 ) );
					assertSame( h1.getFavoriteItem(), h1.getItems().iterator().next() );
					assertThat( h1.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( h1.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
					return h1;
				}
		);

		scope.inTransaction(
				session -> {
					Hoarder h1 = session.get( Hoarder.class, h.getId() );
					assertThat( h1.getItems().size(), is( 1 ) );
					assertSame( h1.getFavoriteItem(), h1.getItems().iterator().next() );
					assertThat( h1.getFavoriteItem().getId(), is( item1.getId() ) );
					assertThat( h1.getFavoriteItem().getCategory(), is( item1.getCategory() ) );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testMergeMultipleEntityCopiesAllowedAndDisallowed(SessionFactoryScope scope) {
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
				session -> {
					Item item = session.get( Item.class, item1.getId() );
					// make sure item1_1.category is initialized
					Hibernate.initialize( item.getCategory() );
					return item;
				}
		);

		scope.inSession(
				session -> {
					session.beginTransaction();
					Item item1Merged = (Item) session.merge( item1 );

					item1Merged.setCategory( category );
					category.setExampleItem( item1_1 );

					// now item1Merged is managed and it has a nested detached item
					// and there is  multiple managed/detached Category objects
					try {
						// the following should fail because multiple copies of Category objects is not allowed by
						// CustomEntityCopyObserver
						session.merge( item1Merged );
						fail( "should have failed because CustomEntityCopyObserver does not allow multiple copies of a Category. " );
					}
					catch (IllegalStateException ex) {
						//expected
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
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

	@SuppressWarnings("unchecked")
	private void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
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
