/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
class CastTest {
	@Test void testCastToString(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			query.select( builder.array(
					builder.literal( 69 ).cast( String.class ),
					builder.literal( 69L ).cast( String.class ),
					builder.literal( 1.0F ).cast( String.class ),
					builder.literal( 1.0D ).cast( String.class )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals( "69", result[0] );
			assertEquals( "69", result[1] );
			assertTrue( "1.0".equals( result[2] ) || "1".equals( result[2] ) );
			assertTrue( "1.0".equals( result[3] ) || "1".equals( result[3] ) );
		} );
	}

	@Test void testCastFromString(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			query.select( builder.array(
					builder.literal( "69" ).cast( Integer.class ),
					builder.literal( "69" ).cast( Long.class ),
					builder.literal( "1.0" ).cast( Float.class ),
					builder.literal( "1.0" ).cast( Double.class )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals( 69, result[0] );
			assertEquals( 69L, result[1] );
			assertEquals(1.0F, result[2] );
			assertEquals(1.0D, result[3] );
		} );
	}
}
