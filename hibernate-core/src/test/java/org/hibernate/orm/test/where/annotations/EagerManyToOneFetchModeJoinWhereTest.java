/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		EagerManyToOneFetchModeJoinWhereTest.Product.class,
		EagerManyToOneFetchModeJoinWhereTest.Category.class
})
@SessionFactory
public class EagerManyToOneFetchModeJoinWhereTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-12104")
	@FailureExpected
	public void testAssociatedWhereClause(SessionFactoryScope factoryScope) {
		var product = new Product();
		var category = new Category();
		category.name = "flowers";
		product.category = category;
		product.containedCategory = new ContainedCategory();
		product.containedCategory.category = category;
		product.containedCategories.add( new ContainedCategory( category ) );

		factoryScope.inTransaction( (session) -> {
			session.persist( product );
		} );

		factoryScope.inTransaction( (session) -> {
			var p = session.find( Product.class, product.id );
			assertNotNull( p );
			assertNotNull( p.category );
			assertNotNull( p.containedCategory.category );
			assertEquals( 1, p.containedCategories.size() );
			Assertions.assertSame( p.category, p.containedCategory.category );
			Assertions.assertSame( p.category, p.containedCategories.iterator().next().category );
		} );

		factoryScope.inTransaction( (session) -> {
			var c = session.find( Category.class, category.id );
			assertNotNull( c );
			c.inactive = 1;
		} );

		factoryScope.inTransaction( (session) -> {
			var c = session.find( Category.class, category.id );
			assertNull( c );
		} );

		factoryScope.inTransaction( (session) -> {
			// Entity-level where clause is ignored when to-one associations to that
			// association is loaded eagerly using FetchMode.JOIN, so the result
			// should be the same as before the Category was made inactive.
			var p = session.find( Product.class, product.id );
			assertNotNull( p );
			assertNull( p.category );
			assertNull( p.containedCategory.category );
			assertEquals( 1, p.containedCategories.size() );
			assertNull( p.containedCategories.iterator().next().category );
		} );
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
