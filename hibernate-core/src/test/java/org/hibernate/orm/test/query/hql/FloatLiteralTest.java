/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(useCollectingStatementInspector = true)
class FloatLiteralTest {
	@Test void test(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inEntityManager( em -> {
			Object halfFloat = em.createQuery( "select 1f/2f" ).getSingleResult();
			assertEquals(0.5f, halfFloat);
			Object halfDouble = em.createQuery( "select 1d/2d" ).getSingleResult();
			assertEquals(0.5d, halfDouble);
		} );
		for ( String sql : statementInspector.getSqlQueries() ) {
			assertTrue( sql.replaceAll( "\\s+", "" ).contains( "(1.0/2.0)" ) );
		}
	}
}
