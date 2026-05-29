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

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.restrict.Restrict;
import jakarta.data.restrict.Restriction;
import org.hibernate.processor.test.integ.model._Product;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@code @Find} methods that accept a {@link Restriction} parameter.
 */
@DomainModel(
		annotatedClasses = { Product.class, ProductStore.class },
		annotatedClassNames = "org.hibernate.processor.test.integ.repository.ProductStore$"
)
@SessionFactory
class RestrictionTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private void seedProducts(_ProductStore store) {
		store.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
		store.save( new Product( 2L, "Gadget", 24.99, 29.99 ) );
		store.save( new Product( 3L, "Gizmo", 49.99, 59.99 ) );
	}

	@Test
	void testFilterUnrestricted(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _ProductStore( session );
			seedProducts( store );

			try ( var stream = store.filter(
					Restrict.unrestricted(),
					Order.by( Sort.asc( "name" ) ) ) ) {
				List<Product> result = stream.toList();
				assertEquals( 3, result.size() );
				assertEquals( "Gadget", result.get( 0 ).getName() );
				assertEquals( "Gizmo", result.get( 1 ).getName() );
				assertEquals( "Widget", result.get( 2 ).getName() );
			}
		} );
	}

	@Test
	void testFilterWithRestriction(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var store = new _ProductStore( session );
			seedProducts( store );

			try ( var stream = store.filter(
					_Product.name.equalTo( "Widget" ),
					Order.by( _Product.name.asc() ) ) ) {
				List<Product> result = stream.toList();
				assertEquals( 1, result.size() );
				assertEquals( "Widget", result.get( 0 ).getName() );
			}
		} );
	}
}
