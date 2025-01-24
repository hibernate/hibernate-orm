/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.limit;

import jakarta.persistence.Query;
import org.hibernate.orm.test.subquery.EntityA;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

public class LimitWithoutOrderByQueryTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class
		};
	}

	@Test
	public void testLimitWithoutOrderByQuery() {
		inSession(
				session -> {
					Query query = session.createQuery(
							"SELECT 1 FROM EntityA a " +
							"WHERE 1 = (SELECT 1 FROM EntityA e " +
							"LIMIT 1)"
					);
				}
		);
	}

	@Test
	public void testLimitWithOrderByQuery() {
		inSession(
				session -> {
					Query query = session.createQuery(
							"SELECT 1 FROM EntityA a " +
							"WHERE 1 = (SELECT 1 FROM EntityA e " +
							"ORDER BY 1 LIMIT 1)"
					);
				}
		);
	}
}
