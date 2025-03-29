/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Product.class, Category.class };
	}

	@Test
	@JiraKey( value = "HHH-12104" )
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

	@Entity(name = "Product")
	public static class Product {
		@Id
		@GeneratedValue
		private int id;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "categoryId")
		@Fetch(FetchMode.JOIN)
		private Category category;

		private ContainedCategory containedCategory;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<ContainedCategory> containedCategories = new HashSet<>();
	}

	@Entity(name = "Category")
	@Table(name = "CATEGORY")
	@SQLRestriction("inactive = 0")
	public static class Category {
		@Id
		@GeneratedValue
		private int id;

		private String name;

		private int inactive;
	}

	@Embeddable
	public static class ContainedCategory {
		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "containedCategoryId")
		@Fetch(FetchMode.JOIN)
		private Category category;

		public ContainedCategory() {
		}

		public ContainedCategory(Category category) {
			this.category = category;
		}
	}
}
