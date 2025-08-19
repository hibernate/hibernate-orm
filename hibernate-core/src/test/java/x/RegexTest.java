/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package x;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
class RegexTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('abcdef', 'ab.*')", Boolean.class ).getSingleResult() );
		} );
	}
}
