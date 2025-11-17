/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests merging multiple detached representations of the same entity when
 * not allowed by default.
 *
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9106")
@Jpa(
		annotatedClasses = {
				Category.class,
				Hoarder.class,
				Item.class
		}
)
public class MergeMultipleEntityCopiesDisallowedByDefaultTest {

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

		// Get another representation of the same Item from a different session.
		Item item1_1 = scope.fromTransaction(
				entityManager -> entityManager.find( Item.class, item1.getId() )
		);

		// item1_1 and item1_2 are unmodified representations of the same persistent entity.
		assertFalse( item1 == item1_1 );
		assertTrue( item1.equals( item1_1 ) );

		// Update hoarder (detached) to reference both representations.
		hoarder.getItems().add( item1 );
		hoarder.setFavoriteItem( item1_1 );

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						entityManager.merge( hoarder );
						fail( "should have failed due to IllegalStateException" );
					}
					catch (IllegalStateException ex) {
						//expected
					}
					finally {
						entityManager.getTransaction().rollback();
					}
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

		scope.inEntityManager(
				entityManager -> {
					Item item1Merged;
					try {
						entityManager.getTransaction().begin();
						item1Merged = entityManager.merge( item1 );

						item1Merged.setCategory( category );
						category.setExampleItem( item1_1 );

						// now item1Merged is managed and it has a nested detached item
						try {
							entityManager.merge( item1Merged );
							fail( "should have failed due to IllegalStateException" );
						}
						catch (IllegalStateException ex) {
							//expected
						}
						finally {
							entityManager.getTransaction().rollback();
						}
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
