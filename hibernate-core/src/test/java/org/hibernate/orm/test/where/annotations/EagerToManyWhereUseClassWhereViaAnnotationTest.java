/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
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
 * Tests association collections with AvailableSettings.USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS = true
 *
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		EagerToManyWhereUseClassWhereViaAnnotationTest.Product.class,
		EagerToManyWhereUseClassWhereViaAnnotationTest.Category.class
})
@SessionFactory
public class EagerToManyWhereUseClassWhereViaAnnotationTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-15936" )
	public void testAssociatedWhereClause(SessionFactoryScope factoryScope) {
		var product = new Product();
		var flowers = new Category();
		flowers.id = 1;
		flowers.name = "flowers";
		flowers.description = "FLOWERS";
		product.categoriesOneToMany.add( flowers );
		product.categoriesWithDescOneToMany.add( flowers );
		product.categoriesManyToMany.add( flowers );
		product.categoriesWithDescManyToMany.add( flowers );
		product.categoriesWithDescIdLt4ManyToMany.add( flowers );
		var vegetables = new Category();
		vegetables.id = 2;
		vegetables.name = "vegetables";
		vegetables.description = "VEGETABLES";
		product.categoriesOneToMany.add( vegetables );
		product.categoriesWithDescOneToMany.add( vegetables );
		product.categoriesManyToMany.add( vegetables );
		product.categoriesWithDescManyToMany.add( vegetables );
		product.categoriesWithDescIdLt4ManyToMany.add( vegetables );
		var dogs = new Category();
		dogs.id = 3;
		dogs.name = "dogs";
		dogs.description = null;
		product.categoriesOneToMany.add( dogs );
		product.categoriesWithDescOneToMany.add( dogs );
		product.categoriesManyToMany.add( dogs );
		product.categoriesWithDescManyToMany.add( dogs );
		product.categoriesWithDescIdLt4ManyToMany.add( dogs );
		var building = new Category();
		building.id = 4;
		building.name = "building";
		building.description = "BUILDING";
		product.categoriesOneToMany.add( building );
		product.categoriesWithDescOneToMany.add( building );
		product.categoriesManyToMany.add( building );
		product.categoriesWithDescManyToMany.add( building );
		product.categoriesWithDescIdLt4ManyToMany.add( building );

		factoryScope.inTransaction( (session) -> {
			session.persist( flowers );
			session.persist( vegetables );
			session.persist( dogs );
			session.persist( building );
			session.persist( product );
		} );

		factoryScope.inTransaction( (session) -> {
			var p = session.find( Product.class, product.id );
			assertNotNull( p );
			assertEquals( 4, p.categoriesOneToMany.size() );
			checkIds( p.categoriesOneToMany, new Integer[] { 1, 2, 3, 4 } );
			assertEquals( 3, p.categoriesWithDescOneToMany.size() );
			checkIds( p.categoriesWithDescOneToMany, new Integer[] { 1, 2, 4 } );
			assertEquals( 4, p.categoriesManyToMany.size() );
			checkIds( p.categoriesManyToMany, new Integer[] { 1, 2, 3, 4 } );
			assertEquals( 3, p.categoriesWithDescManyToMany.size() );
			checkIds( p.categoriesWithDescManyToMany, new Integer[] { 1, 2, 4 } );
			assertEquals( 2, p.categoriesWithDescIdLt4ManyToMany.size() );
			checkIds( p.categoriesWithDescIdLt4ManyToMany, new Integer[] { 1, 2 } );
		} );

		factoryScope.inTransaction( (session) -> {
			var c = session.find( Category.class, flowers.id );
			assertNotNull( c );
			c.inactive = 1;
		} );

		factoryScope.inTransaction( (session) -> {
			var c = session.find( Category.class, flowers.id );
			assertNull( c );
		} );

		factoryScope.inTransaction( (session) -> {
			var p = session.find( Product.class, product.id );
			assertNotNull( p );
			assertEquals( 3, p.categoriesOneToMany.size() );
			checkIds( p.categoriesOneToMany, new Integer[] { 2, 3, 4 } );
			assertEquals( 2, p.categoriesWithDescOneToMany.size() );
			checkIds( p.categoriesWithDescOneToMany, new Integer[] { 2, 4 } );
			assertEquals( 3, p.categoriesManyToMany.size() );
			checkIds( p.categoriesManyToMany, new Integer[] { 2, 3, 4 } );
			assertEquals( 2, p.categoriesWithDescManyToMany.size() );
			checkIds( p.categoriesWithDescManyToMany, new Integer[] { 2, 4 } );
			assertEquals( 1, p.categoriesWithDescIdLt4ManyToMany.size() );
			checkIds( p.categoriesWithDescIdLt4ManyToMany, new Integer[] { 2 } );
		} );
	}

	private void checkIds(Set<Category> categories, Integer[] expectedIds) {
		final Set<Integer> expectedIdSet = new HashSet<>( Arrays.asList( expectedIds ) );
		for ( Category category : categories ) {
			expectedIdSet.remove( category.id );
		}
		assertTrue( expectedIdSet.isEmpty() );
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		@GeneratedValue
		private int id;

		@OneToMany(fetch = FetchType.EAGER)
		@JoinColumn
		private Set<Category> categoriesOneToMany = new HashSet<>();

		@OneToMany(fetch = FetchType.EAGER)
		@JoinColumn
		@SQLRestriction( "description is not null" )
		private Set<Category> categoriesWithDescOneToMany = new HashSet<>();

		@ManyToMany(fetch = FetchType.EAGER)
		@JoinTable(name = "categoriesManyToMany")
		private Set<Category> categoriesManyToMany = new HashSet<>();

		@ManyToMany(fetch = FetchType.EAGER)
		@JoinTable(name = "categoriesWithDescManyToMany", inverseJoinColumns = { @JoinColumn( name = "categoryId" )})
		@SQLRestriction( "description is not null" )
		private Set<Category> categoriesWithDescManyToMany = new HashSet<>();

		@ManyToMany(fetch = FetchType.EAGER)
		@JoinTable(name = "categoriesWithDescIdLt4MToM", inverseJoinColumns = { @JoinColumn( name = "categoryId" )})
		@SQLRestriction( "description is not null" )
		@SQLJoinTableRestriction("categoryId < 4")
		private Set<Category> categoriesWithDescIdLt4ManyToMany = new HashSet<>();
	}

	@Entity(name = "Category")
	@Table(name = "CATEGORY")
	@SQLRestriction("inactive = 0")
	public static class Category {
		@Id
		private int id;

		private String name;

		private String description;

		private int inactive;
	}
}
