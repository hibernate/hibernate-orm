/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Product;
import org.hibernate.processor.test.integ.repository.ProductStore;
import org.hibernate.processor.test.integ.repository._ProductStore;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@code @Query} methods that use the JDQL {@code this} keyword,
 * both on a regular {@code @Repository} and on a companion ({@code $}) interface.
 */
@DomainModel(
		annotatedClasses = { Product.class, ProductStore.class },
		annotatedClassNames = "org.hibernate.processor.test.integ.repository.ProductStore$"
)
@SessionFactory
class ThisQueryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testCountThis(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _ProductStore( session );
			store.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
			store.save( new Product( 2L, "Gadget", 24.99, 29.99 ) );
			store.save( new Product( 3L, "Gizmo", 49.99, 59.99 ) );

			assertEquals( 3, store.countAll() );
		} );
	}

	@Test
	void testSelectThis(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _ProductStore( session );
			store.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
			store.save( new Product( 2L, "Gadget", 24.99, 29.99 ) );
			store.save( new Product( 3L, "Gizmo", 49.99, 59.99 ) );

			List<Product> expensive = store.expensiveThan( 20.0 );
			assertEquals( 2, expensive.size() );
			assertEquals( "Gadget", expensive.get( 0 ).getName() );
			assertEquals( "Gizmo", expensive.get( 1 ).getName() );
		} );
	}

	@Test
	void testSelectThisProperty(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _ProductStore( session );
			store.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
			store.save( new Product( 2L, "Gadget", 24.99, 29.99 ) );
			store.save( new Product( 3L, "Gizmo", 49.99, 59.99 ) );

			List<String> names = store.expensiveNames( 20.0 );
			assertEquals( List.of( "Gadget", "Gizmo" ), names );
		} );
	}

	@Test
	void testCompanionCountThisWithWhere(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _ProductStore( session );
			store.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
			store.save( new Product( 2L, "Gadget", 24.99, 29.99 ) );
			store.save( new Product( 3L, "Gizmo", 49.99, 59.99 ) );

			assertEquals( 1, store.cheaperThan( 20.0 ) );
			assertEquals( 2, store.cheaperThan( 30.0 ) );
			assertEquals( 3, store.cheaperThan( 100.0 ) );
		} );
	}
}
