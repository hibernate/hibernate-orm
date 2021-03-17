/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where.annotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests association collections with default AvailableSettings.USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS,
 * which is true.
 *
 * @author Gail Badner
 */
public class LazyToManyWhereTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Product.class, Category.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13011" )
	public void testAssociatedWhereClause() {

		Product product = new Product();
		Category flowers = new Category();
		flowers.id = 1;
		flowers.name = "flowers";
		flowers.description = "FLOWERS";
		product.categoriesOneToMany.add( flowers );
		product.categoriesWithDescOneToMany.add( flowers );
		product.categoriesManyToMany.add( flowers );
		product.categoriesWithDescManyToMany.add( flowers );
		product.categoriesWithDescIdLt4ManyToMany.add( flowers );
		Category vegetables = new Category();
		vegetables.id = 2;
		vegetables.name = "vegetables";
		vegetables.description = "VEGETABLES";
		product.categoriesOneToMany.add( vegetables );
		product.categoriesWithDescOneToMany.add( vegetables );
		product.categoriesManyToMany.add( vegetables );
		product.categoriesWithDescManyToMany.add( vegetables );
		product.categoriesWithDescIdLt4ManyToMany.add( vegetables );
		Category dogs = new Category();
		dogs.id = 3;
		dogs.name = "dogs";
		dogs.description = null;
		product.categoriesOneToMany.add( dogs );
		product.categoriesWithDescOneToMany.add( dogs );
		product.categoriesManyToMany.add( dogs );
		product.categoriesWithDescManyToMany.add( dogs );
		product.categoriesWithDescIdLt4ManyToMany.add( dogs );
		Category building = new Category();
		building.id = 4;
		building.name = "building";
		building.description = "BUILDING";
		product.categoriesOneToMany.add( building );
		product.categoriesWithDescOneToMany.add( building );
		product.categoriesManyToMany.add( building );
		product.categoriesWithDescManyToMany.add( building );
		product.categoriesWithDescIdLt4ManyToMany.add( building );

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
					Product p = session.get( Product.class, product.id );
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
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Category c = session.get( Category.class, flowers.id );
					assertNotNull( c );
					c.inactive = 1;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Category c = session.get( Category.class, flowers.id );
					assertNull( c );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Product p = session.get( Product.class, product.id );
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
				}
		);
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

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumn
		private Set<Category> categoriesOneToMany = new HashSet<>();

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumn
		@Where( clause = "description is not null" )
		private Set<Category> categoriesWithDescOneToMany = new HashSet<>();

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "categoriesManyToMany")
		private Set<Category> categoriesManyToMany = new HashSet<>();

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "categoriesWithDescManyToMany", inverseJoinColumns = { @JoinColumn( name = "categoryId" )})
		@Where( clause = "description is not null" )
		private Set<Category> categoriesWithDescManyToMany = new HashSet<>();

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "categoriesWithDescIdLt4MToM", inverseJoinColumns = { @JoinColumn( name = "categoryId" )})
		@Where( clause = "description is not null" )
		@WhereJoinTable( clause = "categoryId < 4")
		private Set<Category> categoriesWithDescIdLt4ManyToMany = new HashSet<>();
	}

	@Entity(name = "Category")
	@Table(name = "CATEGORY")
	@Where(clause = "inactive = 0")
	public static class Category {
		@Id
		private int id;

		private String name;

		private String description;

		private int inactive;
	}
}
