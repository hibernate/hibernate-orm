/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

/**
 * Implementation of CurrentSessionConnectionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class CurrentSessionConnectionTest extends AggressiveReleaseTest {
	@Override
	protected Session getSessionUnderTest(SessionFactoryScope scope) {
		return scope.getSessionFactory().getCurrentSession();
	}

	@Override
	protected void release(Session session, SessionFactoryScope scope) {
		// do nothing, txn synch should release session as part of current-session definition
	}
}
