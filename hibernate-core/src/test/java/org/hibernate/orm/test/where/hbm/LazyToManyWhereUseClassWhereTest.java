/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class LazyToManyWhereUseClassWhereTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "where/hbm/LazyToManyWhere.hbm.xml" };
	}

	@Test
	@JiraKey( "HHH-13011" )
	public void testAssociatedWhereClause() {

		Product product = new Product();
		Category flowers = new Category();
		flowers.setId( 1 );
		flowers.setName( "flowers" );
		flowers.setDescription( "FLOWERS" );
		product.getCategoriesOneToMany().add( flowers );
		product.getCategoriesWithDescOneToMany().add( flowers );
		product.getCategoriesManyToMany().add( flowers );
		product.getCategoriesWithDescManyToMany().add( flowers );
		product.getCategoriesWithDescIdLt4ManyToMany().add( flowers );
		Category vegetables = new Category();
		vegetables.setId( 2 );
		vegetables.setName( "vegetables" );
		vegetables.setDescription( "VEGETABLES" );
		product.getCategoriesOneToMany().add( vegetables );
		product.getCategoriesWithDescOneToMany().add( vegetables );
		product.getCategoriesManyToMany().add( vegetables );
		product.getCategoriesWithDescManyToMany().add( vegetables );
		product.getCategoriesWithDescIdLt4ManyToMany().add( vegetables );
		Category dogs = new Category();
		dogs.setId( 3 );
		dogs.setName( "dogs" );
		dogs.setDescription( null );
		product.getCategoriesOneToMany().add( dogs );
		product.getCategoriesWithDescOneToMany().add( dogs );
		product.getCategoriesManyToMany().add( dogs );
		product.getCategoriesWithDescManyToMany().add( dogs );
		product.getCategoriesWithDescIdLt4ManyToMany().add( dogs );
		Category building = new Category();
		building.setId( 4 );
		building.setName( "building" );
		building.setDescription( "BUILDING" );
		product.getCategoriesOneToMany().add( building );
		product.getCategoriesWithDescOneToMany().add( building );
		product.getCategoriesManyToMany().add( building );
		product.getCategoriesWithDescManyToMany().add( building );
		product.getCategoriesWithDescIdLt4ManyToMany().add( building );

		doInHibernate(
				this::sessionFactory,
				session -> {
					session.persist( flowers );
					session.persist( vegetables );
					session.persist( dogs );
					session.persist( building );
					session.persist( product );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Product p = session.get( Product.class, product.getId() );
					assertNotNull( p );
					assertEquals( 4, p.getCategoriesOneToMany().size() );
					checkIds( p.getCategoriesOneToMany(), new Integer[] { 1, 2, 3, 4 } );
					assertEquals( 3, p.getCategoriesWithDescOneToMany().size() );
					checkIds( p.getCategoriesWithDescOneToMany(), new Integer[] { 1, 2, 4 } );
					assertEquals( 4, p.getCategoriesManyToMany().size() );
					checkIds( p.getCategoriesManyToMany(), new Integer[] { 1, 2, 3, 4 } );
					assertEquals( 3, p.getCategoriesWithDescManyToMany().size() );
					checkIds( p.getCategoriesWithDescManyToMany(), new Integer[] { 1, 2, 4 } );
					assertEquals( 2, p.getCategoriesWithDescIdLt4ManyToMany().size() );
					checkIds( p.getCategoriesWithDescIdLt4ManyToMany(), new Integer[] { 1, 2 } );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Category c = session.get( Category.class, flowers.getId() );
					assertNotNull( c );
					c.setInactive( 1 );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Category c = session.get( Category.class, flowers.getId() );
					assertNull( c );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Product p = session.get( Product.class, product.getId() );
					assertNotNull( p );
					assertEquals( 3, p.getCategoriesOneToMany().size() );
					checkIds( p.getCategoriesOneToMany(), new Integer[] { 2, 3, 4 } );
					assertEquals( 2, p.getCategoriesWithDescOneToMany().size() );
					checkIds( p.getCategoriesWithDescOneToMany(), new Integer[] { 2, 4 } );
					assertEquals( 3, p.getCategoriesManyToMany().size() );
					checkIds( p.getCategoriesManyToMany(), new Integer[] { 2, 3, 4 } );
					assertEquals( 2, p.getCategoriesWithDescManyToMany().size() );
					checkIds( p.getCategoriesWithDescManyToMany(), new Integer[] { 2, 4 } );
					assertEquals( 1, p.getCategoriesWithDescIdLt4ManyToMany().size() );
					checkIds( p.getCategoriesWithDescIdLt4ManyToMany(), new Integer[] { 2 } );
				}
		);
	}

	private void checkIds(Set<Category> categories, Integer[] expectedIds) {
		final Set<Integer> expectedIdSet = new HashSet<>( Arrays.asList( expectedIds ) );
		for ( Category category : categories ) {
			expectedIdSet.remove( category.getId() );
		}
		assertTrue( expectedIdSet.isEmpty() );
	}
}
