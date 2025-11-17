/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRegexpLike.class)
class RegexTest {
	@Test
	void testInSelect(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('abcdef', 'ab.*')", Boolean.class ).getSingleResult() );
			assertTrue( em.createQuery( "select 'abcdef' like regexp 'ab.*'", Boolean.class ).getSingleResult() );
			var builder = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			var query = builder.createQuery( Boolean.class );
			query.select( builder.likeRegexp( builder.literal( "abcdef" ), "ab.*" ) );
			assertTrue( em.createQuery( query ).getSingleResult() );
		} );
		scope.inEntityManager( em -> {
			assertFalse( em.createQuery( "select not regexp_like('abcdef', 'ab.*')", Boolean.class ).getSingleResult() );
			assertFalse( em.createQuery( "select 'abcdef' not like regexp 'ab.*'", Boolean.class ).getSingleResult() );
			var builder = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			var query = builder.createQuery( Boolean.class );
			query.select( builder.notLikeRegexp( builder.literal( "abcdef" ), "ab.*" ) );
			assertFalse( em.createQuery( query ).getSingleResult() );
		} );
	}

	@Test
	void testInSelectCaseInsensitive(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('ABCDEF', 'ab.*', 'i')", Boolean.class ).getSingleResult() );
			assertTrue( em.createQuery( "select 'abcdef' ilike regexp 'ab.*'", Boolean.class ).getSingleResult() );
			var builder = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			var query = builder.createQuery( Boolean.class );
			query.select( builder.ilikeRegexp( builder.literal( "ABCDEF" ), "ab.*" ) );
			assertTrue( em.createQuery( query ).getSingleResult() );
		} );
		scope.inEntityManager( em -> {
			assertFalse( em.createQuery( "select not regexp_like('ABCDEF', 'ab.*', 'i')", Boolean.class ).getSingleResult() );
			assertFalse( em.createQuery( "select 'abcdef' not ilike regexp 'ab.*'", Boolean.class ).getSingleResult() );
			var builder = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			var query = builder.createQuery( Boolean.class );
			query.select( builder.notIlikeRegexp( builder.literal( "ABCDEF" ), "ab.*" ) );
			assertFalse( em.createQuery( query ).getSingleResult() );
		} );
	}

	@Test
	void testInWhere(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( 1, em.createQuery( "select 1 where regexp_like('abcdef', 'ab.*')", Integer.class ).getSingleResult() );
			assertEquals( 1, em.createQuery( "select 1 where 'abcdef' like regexp 'ab.*'", Integer.class ).getSingleResult() );
		} );
	}
}
