/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * Implementation of CurrentSessionConnectionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class CurrentSessionConnectionTest extends AggressiveReleaseTest {
	@Override
	protected Session getSessionUnderTest() throws Throwable {
		return sessionFactory().getCurrentSession();
	}

	@Override
	protected void release(Session session) {
		// do nothing, txn synch should release session as part of current-session definition
	}
}
