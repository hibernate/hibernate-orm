/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.txn;

import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.orm.junit.ExtraAssertions;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ResourceLocalTransactionJoiningTest extends AbstractJPATest {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings( builder );
		TestingJtaBootstrap.prepare( builder.getSettings() );
		builder.applySetting(
				AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY,
				JdbcResourceLocalTransactionCoordinatorBuilderImpl.class.getName()
		);
	}

	@Test
	@JiraKey(value = "HHH-9859")
	public void testExpectations() {
		// JPA spec is very vague on what should happen here.  It does vaguely
		// imply that jakarta.persistence.EntityManager.joinTransaction() should only be used
		// for JTA EMs, however it does not enforced that nor does the TCK check that.
		// And the TCK in fact does test calls to jakarta.persistence.EntityManager.isJoinedToTransaction()
		// from resource-local EMs, so lets make sure those work..

		try (Session session = sessionFactory().openSession()) {
			JdbcResourceLocalTransactionCoordinatorImpl tc = ExtraAssertions.assertTyping(
					JdbcResourceLocalTransactionCoordinatorImpl.class,
					( (SessionImplementor) session ).getTransactionCoordinator()
			);
			assertFalse( tc.isJoined() );

			session.beginTransaction();
			try {
				tc = ExtraAssertions.assertTyping(
						JdbcResourceLocalTransactionCoordinatorImpl.class,
						( (SessionImplementor) session ).getTransactionCoordinator()
				);
				assertTrue( tc.isJoined() );
			}
			finally {
				session.getTransaction().rollback();
			}
		}
	}
}
