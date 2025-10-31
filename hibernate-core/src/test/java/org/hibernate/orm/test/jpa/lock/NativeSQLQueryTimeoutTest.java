/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;


import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.jupiter.api.Test;

import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/41335")
@JiraKey( value = "HHH-13493")
@Jpa(
		integrationSettings = {@Setting(name = HINT_SPEC_QUERY_TIMEOUT, value = "500")}
)
public class NativeSQLQueryTimeoutTest {

	@Test
	public void test(EntityManagerFactoryScope scope){
		scope.inTransaction( entityManager -> {
			try {
				entityManager.createNativeQuery(
					"select 1 " +
					"from pg_sleep(2) "
				)
				.getResultList();

				fail("Should have thrown lock timeout exception!");
			} catch (Exception expected) {
				assertTrue(
					ExceptionUtil.rootCause(expected)
						.getMessage().contains("canceling statement due to user request")
				);
			}
		} );
	}
}
