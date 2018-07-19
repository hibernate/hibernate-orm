/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class EagerAssociationWhereTest extends BaseNonConfigCoreFunctionalTestCase {


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Product.class, Category.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12104" )
	//@FailureExpected( jiraKey = "HHH-12104" )
	public void testAssociatedWhereClause() {
		Product product = new Product();
		Category category = new Category();
		category.name = "flowers";
		product.category = category;
		product.containedCategory = new ContainedCategory();
		product.containedCategory.category = category;
		product.categoriesOneToMany.add( category );
		product.categoriesManyToMany.add( category );

		Session session = openSession();
		session.beginTransaction();
		{
					session.persist( product );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		{
					Product p = session.get( Product.class, product.id );
					assertNotNull( p );
					assertNotNull( p.category );
					assertNotNull( p.containedCategory.category );
					assertEquals( 1, p.categoriesOneToMany.size() );
					assertEquals( 1, p.categoriesManyToMany.size() );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		{
					Category c = session.get( Category.class, category.id );
					assertNotNull( c );
					c.inactive = true;
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		{
					Category c = session.get( Category.class, category.id );
					assertNull( c );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		{
					Product p = session.get( Product.class, product.id );
					assertNotNull( p );
					assertEquals( 0, p.categoriesOneToMany.size() );
					assertEquals( 0, p.categoriesManyToMany.size() );
					assertNull( p.category );
					// TODO: it would be nice if p.containedCategory would be null, but that's not
					//       what happens when the embeddable has a single entity property that is not found;
					//       currently, the embeddable is instantiated, and the "not found" entity will be null.
					// assertNull( p.containedCategory );
					assertNull( p.containedCategory.category );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		@GeneratedValue
		private int id;

		@ManyToOne(cascade = CascadeType.ALL)
		@NotFound(action = NotFoundAction.IGNORE)
		private Category category;

		private ContainedCategory containedCategory;

		@OneToMany(fetch = FetchType.EAGER)
		@JoinColumn
		private Set<Category> categoriesOneToMany = new HashSet<>();

		@OneToMany(fetch = FetchType.EAGER)
		private Set<Category> categoriesManyToMany = new HashSet<>();
	}

	@Entity(name = "Category")
	@Table(name = "CATEGORY")
	@Where(clause = "inactive = 0")
	public static class Category {
		@Id
		@GeneratedValue
		private int id;

		private String name;

		private boolean inactive;
	}

	@Embeddable
	public static class ContainedCategory {
		@ManyToOne(fetch = FetchType.EAGER)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "containedCategoryId")
		private Category category;
	}
}
