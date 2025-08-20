/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package x;

import org.hibernate.dialect.HSQLDialect;
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
	@SkipForDialect(dialectClass = SQLServerDialect.class,
			reason = "regexp_like coming in 2025")
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "no regex support in Sybase ASE")
	void testInSelect(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('abcdef', 'ab.*')", Boolean.class ).getSingleResult() );
			assertTrue( em.createQuery( "select 'abcdef' like regexp 'ab.*'", Boolean.class ).getSingleResult() );
		} );
	}
	@Test
	@SkipForDialect(dialectClass = MariaDBDialect.class)
	@SkipForDialect(dialectClass = HSQLDialect.class)
	@SkipForDialect(dialectClass = SQLServerDialect.class,
			reason = "regexp_like coming in 2025")
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "no regex support in Sybase ASE")
	void testInSelectCaseInsensitive(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('ABCDEF', 'ab.*', 'i')", Boolean.class ).getSingleResult() );
			assertTrue( em.createQuery( "select 'abcdef' ilike regexp 'ab.*'", Boolean.class ).getSingleResult() );
		} );
	}
	@Test
	@SkipForDialect(dialectClass = SQLServerDialect.class,
			reason = "regexp_like coming in 2025")
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "no regex support in Sybase ASE")
	void testInWhere(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( 1, em.createQuery( "select 1 where regexp_like('abcdef', 'ab.*')", Integer.class ).getSingleResult() );
			assertEquals( 1, em.createQuery( "select 1 where 'abcdef' like regexp 'ab.*'", Integer.class ).getSingleResult() );
		} );
	}
}
