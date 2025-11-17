/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		Item.class
})
public class ElementCollectionConverterTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12581")
	public void testCriteriaQueryWithElementCollectionUsingConverter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Item item1 = new Item( "P1" );
					item1.getRoles().add( new Color() );

					Item item2 = new Item( "P2" );
					item2.getRoles().add( new Industry() );

					Item item3 = new Item( "P3" );
					item3.getRoles().add( new Color() );
					item3.getRoles().add( new Industry() );

					entityManager.persist( item1 );
					entityManager.persist( item2 );
					entityManager.persist( item3 );
				}
		);

		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Item> query = cb.createQuery( Item.class );
					Root<Item> root = query.from( Item.class );

					// HHH-12338 effectively caused Item_.roles to be null.
					// Therefore this caused a NPE with the commit originally applied for HHH-12338.
					// Reverting that fix avoids the regression and this proceeds as expected.
					root.fetch( Item_.roles );

					// Just running the query here.
					// the outcome is less important than the above for context of this test case.
					query = query.select( root ).distinct( true );
					List<Item> items = entityManager.createQuery( query ).getResultList();
					assertEquals( 3, items.size() );
				}
		);
	}
}
