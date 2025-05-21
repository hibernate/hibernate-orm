/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa
class LeftRightReplaceTest {
	@Test void testLeftRightReplace(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			var builder = entityManager.getCriteriaBuilder();
			var query = builder.createQuery( Object[].class );
			query.select( builder.array(
					builder.left( builder.literal( "Hibernate in Action" ), 9 ),
					builder.right( builder.literal( "Hibernate in Action" ), 6 ),
					builder.replace( builder.literal( "Hibernate in Action" ), "Action", "Quarkus" )
			) );
			var result = entityManager.createQuery( query ).getSingleResult();
			assertEquals( "Hibernate", result[0] );
			assertEquals( "Action", result[1] );
			assertEquals("Hibernate in Quarkus", result[2] );
		} );
	}
}
