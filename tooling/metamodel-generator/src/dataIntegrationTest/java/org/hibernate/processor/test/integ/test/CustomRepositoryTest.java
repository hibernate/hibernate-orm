/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Product;
import org.hibernate.processor.test.integ.repository.CustomProductStore;
import org.hibernate.processor.test.integ.repository._CustomProductStore;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests a repository that does not extend any built-in repository supertype.
 * The primary entity class is inferred from lifecycle method parameter types.
 */
@DomainModel(
		annotatedClasses = { Product.class, CustomProductStore.class }
)
@SessionFactory
class CustomRepositoryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testCountAll(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _CustomProductStore( session );
			store.add( List.of(
					new Product( 1L, "Widget", 9.99, 14.99 ),
					new Product( 2L, "Gadget", 24.99, 29.99 )
			) );

			assertEquals( 2, store.countAll() );
		} );
	}

	@Test
	void testInsertAndDelete(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _CustomProductStore( session );
			var products = List.of(
					new Product( 1L, "Widget", 9.99, 14.99 ),
					new Product( 2L, "Gadget", 24.99, 29.99 ),
					new Product( 3L, "Gizmo", 49.99, 59.99 )
			);
			store.add( products );
			assertEquals( 3, store.countAll() );

			store.remove( List.of( products.get( 1 ) ) );
			assertEquals( 2, store.countAll() );
		} );
	}
}
