/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Product;
import org.hibernate.processor.test.integ.repository.Catalog;
import org.hibernate.processor.test.integ.repository._Catalog;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = { Product.class, Catalog.class },
		annotatedClassNames = "org.hibernate.processor.test.integ.repository.Catalog$"
)
@SessionFactory
class CatalogTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testSaveAndFindByName(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _Catalog( session );
			catalog.save( new Product( 1L, "Widget", 9.99, 14.99 ) );
			catalog.save( new Product( 2L, "Widget", 19.99, 24.99 ) );
			catalog.save( new Product( 3L, "Gadget", 29.99, 34.99 ) );

			List<Product> widgets = catalog.findByName( "Widget" );
			assertEquals( 2, widgets.size() );
		} );
	}

	@Test
	void testCountByPriceGreaterThanEqual(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _Catalog( session );
			catalog.save( new Product( 1L, "Cheap", 5.0, 7.0 ) );
			catalog.save( new Product( 2L, "Mid", 15.0, 20.0 ) );
			catalog.save( new Product( 3L, "Expensive", 50.0, 60.0 ) );

			assertEquals( 2, catalog.countByPriceGreaterThanEqual( 10.0 ) );
			assertEquals( 1, catalog.countByPriceGreaterThanEqual( 40.0 ) );
			assertEquals( 3, catalog.countByPriceGreaterThanEqual( 1.0 ) );
		} );
	}

	@Test
	void testDeleteById(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _Catalog( session );
			catalog.save( new Product( 1L, "ToDelete", 10.0, 15.0 ) );
			catalog.save( new Product( 2L, "ToKeep", 20.0, 25.0 ) );

			assertEquals( 2, catalog.countByPriceGreaterThanEqual( 0.0 ) );
			catalog.deleteById( 1L );
			assertEquals( 1, catalog.countByPriceGreaterThanEqual( 0.0 ) );

			List<Product> remaining = catalog.findByName( "ToKeep" );
			assertEquals( 1, remaining.size() );
		} );
	}
}
