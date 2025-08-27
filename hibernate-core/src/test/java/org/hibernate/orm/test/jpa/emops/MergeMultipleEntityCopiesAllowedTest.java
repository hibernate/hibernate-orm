/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;


import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests merging multiple detached representations of the same entity when it is explicitly allowed.
 *
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9106")
@Jpa(
		annotatedClasses = {
				Category.class,
				Hoarder.class,
				Item.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "allow") }
)
public class MergeMultipleEntityCopiesAllowedTest {

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCascadeFromDetachedToNonDirtyRepresentations(EntityManagerFactoryScope scope) {
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

		// Update hoarder (detached) to reference both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		Hoarder hoarder2 = scope.fromTransaction(
				entityManager -> {
					Hoarder _hoarder = entityManager.merge( hoarder );
					assertEquals( 1, _hoarder.getItems().size() );
					assertSame( _hoarder.getFavoriteItem(), _hoarder.getItems().iterator().next() );
					assertEquals( item1.getId(), _hoarder.getFavoriteItem().getId() );
					assertEquals( item1.getCategory(), _hoarder.getFavoriteItem().getCategory() );
					return _hoarder;
				}
		);

		scope.inTransaction(
				entityManager -> {
					Hoarder _hoarder = entityManager.merge( hoarder2 );
					assertEquals( 1, _hoarder.getItems().size() );
					assertSame( _hoarder.getFavoriteItem(), _hoarder.getItems().iterator().next() );
					assertEquals( item1.getId(), _hoarder.getFavoriteItem().getId() );
					assertEquals( item1.getCategory(), _hoarder.getFavoriteItem().getCategory() );
				}
		);
	}


	@Test
	public void testTopLevelManyToOneManagedNestedIsDetached(EntityManagerFactoryScope scope) {
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
				entityManager -> entityManager.find( Item.class, item1.getId() )
		);

		scope.inTransaction(
				entityManager -> {
					Item item1Merged = entityManager.merge( item1 );

					item1Merged.setCategory( category );
					category.setExampleItem( item1_1 );

					// now item1Merged is managed and it has a nested detached item
					entityManager.merge( item1Merged );
					assertEquals( category.getName(), item1Merged.getCategory().getName() );
					assertSame( item1Merged, item1Merged.getCategory().getExampleItem() );
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
}
