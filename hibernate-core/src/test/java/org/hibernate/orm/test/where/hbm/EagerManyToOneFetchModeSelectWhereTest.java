/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import java.util.HashSet;
import java.util.Set;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name=IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa"))
@DomainModel(xmlMappings = "hbm/where/EagerManyToOneFetchModeSelectWhereTest.hbm.xml")
@SessionFactory
public class EagerManyToOneFetchModeSelectWhereTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-12104" )
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
			assertSame( p.category, p.containedCategory.category );
			assertSame( p.category, p.containedCategories.iterator().next().category );
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
			// Entity-level where clause is taken into account when to-one associations
			// to that entity is loaded eagerly using FetchMode.SELECT, so Category
			// associations will be null.
			var p = session.find( Product.class, product.id );
			assertNotNull( p );
			assertNull( p.category );
			assertNull( p.containedCategory );
			assertEquals( 0, p.containedCategories.size() );
		} );
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
