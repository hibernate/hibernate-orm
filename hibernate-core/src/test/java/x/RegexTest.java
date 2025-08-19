/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package x;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
class RegexTest {
	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 19)
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 21)
	@SkipForDialect(dialectClass = SQLServerDialect.class)
	void testInSelect(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('abcdef', 'ab.*')", Boolean.class ).getSingleResult() );
		} );
	}
	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class)
	@SkipForDialect(dialectClass = MariaDBDialect.class)
	void testInSelectCaseInsensitive(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('ABCDEF', 'ab.*', 'i')", Boolean.class ).getSingleResult() );
		} );
	}
	@Test
	void testInWhere(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( 1, em.createQuery( "select 1 where regexp_like('abcdef', 'ab.*')", Integer.class ).getSingleResult() );
		} );
	}
}
