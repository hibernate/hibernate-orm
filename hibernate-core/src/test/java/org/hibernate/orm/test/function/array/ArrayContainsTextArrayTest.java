/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.List;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = ArrayContainsTextArrayTest.Item.class)
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
@JiraKey("HHH-20695")
public class ArrayContainsTextArrayTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1L, new String[]{ "alpha", "beta" } ) );
			session.persist( new Item( 2L, new String[]{ "gamma" } ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testArrayContains(SessionFactoryScope scope) {
		scope.inSession( session -> {
			List<Item> results = session.createQuery(
					"from Item i where array_contains(i.tags, :tag)",
					Item.class
			).setParameter( "tag", "alpha" ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 1L, results.get( 0 ).getId() );

			results = session.createQuery(
					"from Item i where array_contains(i.tags, :tag)",
					Item.class
			).setParameter( "tag", "missing" ).getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testContainsSyntax(SessionFactoryScope scope) {
		scope.inSession( session -> {
			List<Item> results = session.createQuery(
					"from Item i where i.tags contains 'beta'",
					Item.class
			).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 1L, results.get( 0 ).getId() );
		} );
	}

	@Entity(name = "Item")
	@Table(name = "hh20695_item")
	public static class Item {
		@Id
		private Long id;

		@Column(name = "tags", columnDefinition = "text[]")
		private String[] tags;

		public Item() {
		}

		public Item(Long id, String[] tags) {
			this.id = id;
			this.tags = tags;
		}

		public Long getId() {
			return id;
		}

		public String[] getTags() {
			return tags;
		}
	}
}
