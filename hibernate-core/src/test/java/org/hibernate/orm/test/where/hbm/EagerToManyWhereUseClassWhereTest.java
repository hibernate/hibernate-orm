/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "hbm/where/EagerToManyWhere.hbm.xml")
@SessionFactory
public class EagerToManyWhereUseClassWhereTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( "HHH-13011" )
	public void testAssociatedWhereClause(SessionFactoryScope factoryScope) {
		var product = new Product();
		var flowers = new Category();
		flowers.setId( 1 );
		flowers.setName( "flowers" );
		flowers.setDescription( "FLOWERS" );
		product.getCategoriesOneToMany().add( flowers );
		product.getCategoriesWithDescOneToMany().add( flowers );
		product.getCategoriesManyToMany().add( flowers );
		product.getCategoriesWithDescManyToMany().add( flowers );
		product.getCategoriesWithDescIdLt4ManyToMany().add( flowers );
		var vegetables = new Category();
		vegetables.setId( 2 );
		vegetables.setName( "vegetables" );
		vegetables.setDescription( "VEGETABLES" );
		product.getCategoriesOneToMany().add( vegetables );
		product.getCategoriesWithDescOneToMany().add( vegetables );
		product.getCategoriesManyToMany().add( vegetables );
		product.getCategoriesWithDescManyToMany().add( vegetables );
		product.getCategoriesWithDescIdLt4ManyToMany().add( vegetables );
		var dogs = new Category();
		dogs.setId( 3 );
		dogs.setName( "dogs" );
		dogs.setDescription( null );
		product.getCategoriesOneToMany().add( dogs );
		product.getCategoriesWithDescOneToMany().add( dogs );
		product.getCategoriesManyToMany().add( dogs );
		product.getCategoriesWithDescManyToMany().add( dogs );
		product.getCategoriesWithDescIdLt4ManyToMany().add( dogs );
		var building = new Category();
		building.setId( 4 );
		building.setName( "building" );
		building.setDescription( "BUILDING" );
		product.getCategoriesOneToMany().add( building );
		product.getCategoriesWithDescOneToMany().add( building );
		product.getCategoriesManyToMany().add( building );
		product.getCategoriesWithDescManyToMany().add( building );
		product.getCategoriesWithDescIdLt4ManyToMany().add( building );

		factoryScope.inTransaction( (session) -> {
			session.persist( flowers );
			session.persist( vegetables );
			session.persist( dogs );
			session.persist( building );
			session.persist( product );
		} );

		factoryScope.inTransaction( (session) -> {
			var p = session.find( Product.class, product.getId() );
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
		} );

		factoryScope.inTransaction( (session) -> {
			var c = session.find( Category.class, flowers.getId() );
			assertNotNull( c );
			c.setInactive( 1 );
		} );

		factoryScope.inTransaction( (session) -> {
			var c = session.find( Category.class, flowers.getId() );
			assertNull( c );
		} );

		factoryScope.inTransaction( (session) -> {
			var p = session.find( Product.class, product.getId() );
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
		} );
	}

	private void checkIds(Set<Category> categories, Integer[] expectedIds) {
		final Set<Integer> expectedIdSet = new HashSet<>( Arrays.asList( expectedIds ) );
		for ( Category category : categories ) {
			expectedIdSet.remove( category.getId() );
		}
		assertTrue( expectedIdSet.isEmpty() );
	}
}
