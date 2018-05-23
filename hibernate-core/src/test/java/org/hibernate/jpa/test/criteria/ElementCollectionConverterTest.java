/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class ElementCollectionConverterTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Item.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12581")
	public void testCriteriaQueryWithElementCollectionUsingConverter() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
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
		} );
	}
}
