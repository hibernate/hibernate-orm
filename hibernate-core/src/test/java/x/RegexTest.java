/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package x;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
class RegexTest {
	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 19)
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 21)
	@SkipForDialect(dialectClass = SybaseASEDialect.class)
	@SkipForDialect(dialectClass = SQLServerDialect.class)
	void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertTrue( em.createQuery( "select regexp_like('abcdef', 'ab.*')", Boolean.class ).getSingleResult() );
		} );
	}
}
