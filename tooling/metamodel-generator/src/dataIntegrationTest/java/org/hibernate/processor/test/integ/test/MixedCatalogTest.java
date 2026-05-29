/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Product;
import org.hibernate.processor.test.integ.repository.MixedCatalog;
import org.hibernate.processor.test.integ.repository._MixedCatalog;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the companion pattern where the main interface has its own
 * {@code @Query} methods AND the companion provides additional
 * {@code @Query} overrides for un-annotated methods.
 */
@DomainModel(
		annotatedClasses = { Product.class, MixedCatalog.class },
		annotatedClassNames = "org.hibernate.processor.test.integ.repository.MixedCatalog$"
)
@SessionFactory
class MixedCatalogTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testQueryFromMainInterface(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _MixedCatalog( session );
			catalog.save( new Product( 1L, "Alpha", 5.0, 7.0 ) );
			catalog.save( new Product( 2L, "Beta", 15.0, 20.0 ) );
			catalog.save( new Product( 3L, "Gamma", 50.0, 60.0 ) );

			List<Product> results = catalog.findByPriceRange( 10.0, 40.0 );
			assertEquals( 1, results.size() );
			assertEquals( "Beta", results.get( 0 ).getName() );
		} );
	}

	@Test
	void testQueryFromCompanionOverride(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _MixedCatalog( session );
			catalog.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
			catalog.save( new Product( 2L, "Widget", 19.99, 24.99 ) );
			catalog.save( new Product( 3L, "Gadget", 29.99, 34.99 ) );

			List<Product> widgets = catalog.findByName( "Widget" );
			assertEquals( 2, widgets.size() );
		} );
	}

	@Test
	void testCountFromCompanionOverride(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _MixedCatalog( session );
			catalog.save( new Product( 1L, "Cheap", 5.0, 7.0 ) );
			catalog.save( new Product( 2L, "Mid", 15.0, 20.0 ) );
			catalog.save( new Product( 3L, "Expensive", 50.0, 60.0 ) );

			assertEquals( 2, catalog.countByPriceGreaterThanEqual( 10.0 ) );
		} );
	}
}
