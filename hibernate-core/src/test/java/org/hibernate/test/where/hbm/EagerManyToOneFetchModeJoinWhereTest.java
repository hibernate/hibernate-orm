/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where.hbm;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Gail Badner
 */
public class EagerManyToOneFetchModeJoinWhereTest extends BaseNonConfigCoreFunctionalTestCase {

	protected String[] getMappings() {
		return new String[] { "where/hbm/EagerManyToOneFetchModeJoinWhereTest.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12104" )
	@FailureExpected( jiraKey = "HHH-12104")
	public void testAssociatedWhereClause() {
		Product product = new Product();
		Category category = new Category();
		category.name = "flowers";
		product.category = category;
		product.containedCategory = new ContainedCategory();
		product.containedCategory.category = category;
		product.containedCategories.add( new ContainedCategory( category ) );

		doInHibernate(
				this::sessionFactory,
				session -> {
					session.persist( product );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Product p = session.get( Product.class, product.id );
					assertNotNull( p );
					assertNotNull( p.category );
					assertNotNull( p.containedCategory.category );
					assertEquals( 1, p.containedCategories.size() );
					assertSame( p.category, p.containedCategory.category );
					assertSame( p.category, p.containedCategories.iterator().next().category );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Category c = session.get( Category.class, category.id );
					assertNotNull( c );
					c.inactive = 1;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					Category c = session.get( Category.class, category.id );
					assertNull( c );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					// Entity's where clause is ignored when to-one associations to that
					// association is loaded eagerly using FetchMode.JOIN, so the result
					// should be the same as before the Category was made inactive.
					Product p = session.get( Product.class, product.id );
					assertNotNull( p );
					assertNull( p.category );
					assertNull( p.containedCategory.category );
					assertEquals( 1, p.containedCategories.size() );
					assertNull( p.containedCategories.iterator().next().category );
				}
		);
	}

	public static class Product {
		private int id;

		private Category category;

		private ContainedCategory containedCategory;

		private Set<ContainedCategory> containedCategories = new HashSet<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public ContainedCategory getContainedCategory() {
			return containedCategory;
		}

		public void setContainedCategory(ContainedCategory containedCategory) {
			this.containedCategory = containedCategory;
		}

		public Set<ContainedCategory> getContainedCategories() {
			return containedCategories;
		}

		public void setContainedCategories(Set<ContainedCategory> containedCategories) {
			this.containedCategories = containedCategories;
		}
	}

	public static class Category {
		private int id;

		private String name;

		private int inactive;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getInactive() {
			return inactive;
		}

		public void setInactive(int inactive) {
			this.inactive = inactive;
		}
	}

	public static class ContainedCategory {
		private Category category;

		public ContainedCategory() {
		}

		public ContainedCategory(Category category) {
			this.category = category;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}
	}
}
