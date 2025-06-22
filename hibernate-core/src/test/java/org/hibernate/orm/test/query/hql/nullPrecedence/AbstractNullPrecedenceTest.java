/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.nullPrecedence;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
abstract class AbstractNullPrecedenceTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ExampleEntity entity1 = new ExampleEntity( 1L );
			entity1.setName( "name1" );
			session.persist( entity1 );

			ExampleEntity entity2 = new ExampleEntity( 2L );
			session.persist( entity2 );

			ExampleEntity entity3 = new ExampleEntity( 3L );
			entity3.setName( "name3" );
			session.persist( entity3 );

		} );
	}

	@Test
	void testNullPrecedenceInOrdering(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<ExampleEntity> query = session.createQuery( "select e from ExampleEntity e order by e.name asc nulls first", ExampleEntity.class );
			List<ExampleEntity> exampleEntities = query.getResultList();
			assertThat( exampleEntities.stream().map( ExampleEntity::getName ).collect( Collectors.toList() ),
						equalTo( Arrays.asList( null, "name1", "name3" ) ) );

			query = session.createQuery( "select e from ExampleEntity e order by e.name asc nulls last", ExampleEntity.class );
			exampleEntities = query.getResultList();
			assertThat( exampleEntities.stream().map( ExampleEntity::getName ).collect( Collectors.toList() ),
						equalTo( Arrays.asList( "name1", "name3", null ) ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
