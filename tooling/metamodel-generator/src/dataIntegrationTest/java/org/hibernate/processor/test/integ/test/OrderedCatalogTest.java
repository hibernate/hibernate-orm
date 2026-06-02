/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Product;
import org.hibernate.processor.test.integ.repository.OrderedCatalog;
import org.hibernate.processor.test.integ.repository._OrderedCatalog;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = { Product.class, OrderedCatalog.class },
		annotatedClassNames = "org.hibernate.processor.test.integ.repository.OrderedCatalog$"
)
@SessionFactory
class OrderedCatalogTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testFindByPriceOrderByName(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _OrderedCatalog( session );
			catalog.save( new Product( 1L, "Banana", 15.0, 7.0 ) );
			catalog.save( new Product( 2L, "Apple", 15.0, 20.0 ) );
			catalog.save( new Product( 3L, "Cherry", 15.0, 18.0 ) );

			List<Product> results = catalog.findByPriceGreaterThanEqualOrderByNameAsc( 15.0 );
			assertEquals( 3, results.size() );
			assertEquals( "Apple", results.get( 0 ).getName() );
			assertEquals( "Banana", results.get( 1 ).getName() );
			assertEquals( "Cherry", results.get( 2 ).getName() );
		} );
	}

	@Test
	void testFindByPriceOrderById(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _OrderedCatalog( session );
			catalog.save( new Product( 3L, "Cherry", 15.0, 18.0 ) );
			catalog.save( new Product( 1L, "Banana", 15.0, 7.0 ) );
			catalog.save( new Product( 2L, "Apple", 15.0, 20.0 ) );

			List<Product> results = catalog.findByPriceGreaterThanEqualOrderByIdAsc( 15.0 );
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).getId() );
			assertEquals( 2L, results.get( 1 ).getId() );
			assertEquals( 3L, results.get( 2 ).getId() );
		} );
	}

	@Test
	void testFindByPriceStreamOrderById(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _OrderedCatalog( session );
			catalog.save( new Product( 3L, "Cherry", 15.0, 18.0 ) );
			catalog.save( new Product( 1L, "Banana", 15.0, 7.0 ) );
			catalog.save( new Product( 2L, "Apple", 15.0, 20.0 ) );

			var results = catalog.findByPriceGreaterThanEqual( 15.0 ).toList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).getId() );
			assertEquals( 2L, results.get( 1 ).getId() );
			assertEquals( 3L, results.get( 2 ).getId() );
		} );
	}

	@Test
	void testCountByPrice(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var catalog = new _OrderedCatalog( session );
			catalog.save( new Product( 1L, "Cheap", 5.0, 7.0 ) );
			catalog.save( new Product( 2L, "Mid", 15.0, 20.0 ) );
			catalog.save( new Product( 3L, "Expensive", 50.0, 60.0 ) );

			assertEquals( 2, catalog.countByPriceGreaterThanEqual( 10.0 ) );
		} );
	}
}
