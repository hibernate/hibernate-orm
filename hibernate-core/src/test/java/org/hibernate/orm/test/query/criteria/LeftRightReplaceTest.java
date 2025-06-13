/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa
class LeftRightReplaceTest {
	@Test
	void testLeftRight(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			query.select( builder.array(
					builder.left( builder.literal( "Hibernate in Action" ), 9 ),
					builder.right( builder.literal( "Hibernate in Action" ), 6 )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals( "Hibernate", result[0] );
			assertEquals( "Action", result[1] );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = FirebirdDialect.class, reason = "Firebird produces a truncation error as replace infers the maximum length from the type of the first argument" )
	void testReplace(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			query.select( builder.array(
					builder.replace( builder.literal( "Hibernate in Action" ), "Action", "Quarkus" )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals("Hibernate in Quarkus", result[0] );
		} );
	}
}
