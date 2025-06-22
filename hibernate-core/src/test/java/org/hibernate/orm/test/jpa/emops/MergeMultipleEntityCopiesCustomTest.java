/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;


import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity using a custom EntityCopyObserver.
 *
 * @author Gail Badner
 */
@JiraKey( value = "HHH-9106")
@Jpa(
		annotatedClasses = {
				Category.class,
				Hoarder.class,
				Item.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "org.hibernate.orm.test.jpa.emops.MergeMultipleEntityCopiesCustomTest$CustomEntityCopyObserver") }
)
public class MergeMultipleEntityCopiesCustomTest {

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeMultipleEntityCopiesAllowed(EntityManagerFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1" );

		Hoarder hoarder = new Hoarder();
		hoarder.setName( "joe" );
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( item1 );
					entityManager.persist( hoarder );
				}
		);

		// Get another representation of the same Item from a different EntityManager.

		Item item1_1 = scope.fromTransaction(
				entityManager -> entityManager.find( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_1 );
		assertTrue( item1.equals( item1_1 ) );

		// Update hoarder (detached) to references both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		scope.inTransaction(
				entityManager -> {
					// the merge should succeed because it does not have Category copies.
					// (CustomEntityCopyObserver does not allow Category copies; it does allow Item copies)
					Hoarder _hoarder = entityManager.merge( hoarder );
					assertEquals( 1, _hoarder.getItems().size() );
					assertSame( _hoarder.getFavoriteItem(), _hoarder.getItems().iterator().next() );
					assertEquals( item1.getId(), _hoarder.getFavoriteItem().getId() );
					assertEquals( item1.getCategory(), _hoarder.getFavoriteItem().getCategory() );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Hoarder _hoarder = entityManager.find( Hoarder.class, hoarder.getId() );
					assertEquals( 1, _hoarder.getItems().size() );
					assertSame( _hoarder.getFavoriteItem(), _hoarder.getItems().iterator().next() );
					assertEquals( item1.getId(), _hoarder.getFavoriteItem().getId() );
					assertEquals( item1.getCategory(), _hoarder.getFavoriteItem().getCategory() );
				}
		);
	}

	@Test
	public void testMergeMultipleEntityCopiesAllowedAndDisallowed(EntityManagerFactoryScope scope) {
		Item item1 = new Item();
		item1.setName( "item1 name" );
		Category category = new Category();
		category.setName( "category" );
		item1.setCategory( category );
		category.setExampleItem( item1 );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( item1 );
				}
		);

		// get another representation of item1
		Item item1_1 = scope.fromTransaction(
				entityManager -> {
					Item _item1_1 = entityManager.find( Item.class, item1.getId() );
					// make sure item1_1.category is initialized
					Hibernate.initialize( _item1_1.getCategory() );
					return _item1_1;
				}
		);

		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Item item1Merged = entityManager.merge( item1 );

						// make sure item1Merged.category is also managed
						Hibernate.initialize( item1Merged.getCategory() );

						item1Merged.setCategory( category );
						category.setExampleItem( item1_1 );

						// now item1Merged is managed and it has a nested detached item
						// and there is  multiple managed/detached Category objects
						try {
							// the following should fail because multiple copies of Category objects is not allowed by
							// CustomEntityCopyObserver
							entityManager.merge( item1Merged );
							fail( "should have failed because CustomEntityCopyObserver does not allow multiple copies of a Category. " );
						}
						catch (IllegalStateException ex) {
							// expected
						}
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);

		scope.inTransaction(
				entityManager -> {
					Item _item1 = entityManager.find( Item.class, item1.getId() );
					assertEquals( category.getName(), _item1.getCategory().getName() );
					assertSame( _item1, _item1.getCategory().getExampleItem() );
				}
		);
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
