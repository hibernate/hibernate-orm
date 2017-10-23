/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.txn;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.test.jpa.AbstractJPATest;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ResourceLocalTransactionJoiningTest extends AbstractJPATest {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		TestingJtaBootstrap.prepare( cfg.getProperties() );
		cfg.setProperty(
				AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY,
				JdbcResourceLocalTransactionCoordinatorBuilderImpl.class.getName()
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9859" )
	public void testExpectations() {
		// JPA spec is very vague on what should happen here.  It does vaguely
		// imply that javax.persistence.EntityManager.joinTransaction() should only be used
		// for JTA EMs, however it does not enforced that nor does the TCK check that.
		// And the TCK in fact does test calls to javax.persistence.EntityManager.isJoinedToTransaction()
		// from resource-local EMs, so lets make sure those work..

		Session session = sessionFactory().openSession();
		JdbcResourceLocalTransactionCoordinatorImpl tc = ExtraAssertions.assertTyping(
				JdbcResourceLocalTransactionCoordinatorImpl.class,
				( (SessionImplementor) session ).getTransactionCoordinator()
		);
		assertFalse( tc.isJoined() );

		session.beginTransaction();
		tc = ExtraAssertions.assertTyping(
				JdbcResourceLocalTransactionCoordinatorImpl.class,
				( (SessionImplementor) session ).getTransactionCoordinator()
		);
		assertTrue( tc.isJoined() );

		session.getTransaction().rollback();
		session.close();
	}
}
