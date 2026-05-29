/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Rectangle;
import org.hibernate.processor.test.integ.repository.RectangleRepository;
import org.hibernate.processor.test.integ.repository._RectangleRepository;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests a repository backed by an entity whose primitive fields
 * carry bean-validation annotations ({@code @PositiveOrZero},
 * {@code @Max}, {@code @NotBlank}).
 */
@DomainModel(
		annotatedClasses = { Rectangle.class, RectangleRepository.class }
)
@SessionFactory
class RectangleRepositoryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testSaveAndFindById(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _RectangleRepository( session );
			repo.save( new Rectangle( "r1", 10, 20, 100, 50 ) );

			var found = repo.findById( "r1" );
			assertTrue( found.isPresent() );
			assertEquals( 100, found.get().getWidth() );
			assertEquals( 50, found.get().getHeight() );
		} );
	}

	@Test
	void testFindByPosition(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _RectangleRepository( session );
			repo.save( new Rectangle( "a", 0, 0, 10, 10 ) );
			repo.save( new Rectangle( "b", 100, 200, 30, 40 ) );
			repo.save( new Rectangle( "c", 100, 200, 50, 60 ) );

			List<Rectangle> atOrigin = repo.findByPosition( 0, 0 );
			assertEquals( 1, atOrigin.size() );
			assertEquals( "a", atOrigin.get( 0 ).getId() );

			List<Rectangle> atPoint = repo.findByPosition( 100, 200 );
			assertEquals( 2, atPoint.size() );
		} );
	}

	@Test
	void testLargerThan(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _RectangleRepository( session );
			repo.save( new Rectangle( "small", 0, 0, 5, 5 ) );
			repo.save( new Rectangle( "medium", 0, 0, 50, 30 ) );
			repo.save( new Rectangle( "large", 0, 0, 200, 100 ) );

			List<Rectangle> results = repo.largerThan( 40, 20 );
			assertEquals( 2, results.size() );
			assertEquals( "large", results.get( 0 ).getId() );
			assertEquals( "medium", results.get( 1 ).getId() );
		} );
	}

	@Test
	void testCountWithAreaAtLeast(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _RectangleRepository( session );
			repo.save( new Rectangle( "tiny", 0, 0, 2, 3 ) );       // area = 6
			repo.save( new Rectangle( "mid", 0, 0, 10, 10 ) );      // area = 100
			repo.save( new Rectangle( "big", 0, 0, 100, 200 ) );    // area = 20000

			assertEquals( 3, repo.countWithAreaAtLeast( 1 ) );
			assertEquals( 2, repo.countWithAreaAtLeast( 50 ) );
			assertEquals( 1, repo.countWithAreaAtLeast( 1000 ) );
		} );
	}
}
