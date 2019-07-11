/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.lock;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.util.ExceptionUtil;
import org.hibernate.type.IntegerType;
import org.junit.Test;

import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
@TestForIssue( jiraKey = "HHH-13493")
public class NativeSQLQueryTimeoutTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( QueryHints.SPEC_HINT_TIMEOUT, "500" );
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
