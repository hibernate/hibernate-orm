/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.util.Map;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@SkipForDialect(value = CockroachDialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/41335")
@JiraKey( value = "HHH-13493")
public class NativeSQLQueryTimeoutTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( HINT_SPEC_QUERY_TIMEOUT, "500" );
	}

	@Test
	public void test(){
		doInJPA( this::entityManagerFactory, entityManager -> {
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

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
		};
	}
}
