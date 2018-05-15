/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.resource.transaction.jta;

import java.util.Map;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13076")
public class JpaComplianceAlreadyStartedTransactionTest extends BaseNonConfigCoreFunctionalTestCase {
	private TransactionManager tm;

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		settings.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
	}

	@Before
	public void setUp() {
		tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
	}

	@Test(expected = IllegalStateException.class)
	public void anIllegalStateExceptionShouldBeThrownWhenBeginTxIsCalledWithAnAlreadyActiveTX() throws Exception {
		try (Session s = openSession()) {
			tm.begin();
			Transaction tx = null;
			try {
				// A call to begin() with an active Tx should cause an IllegalStateException
				tx = s.beginTransaction();
			}
			catch (Exception e) {
				if ( tx != null && tx.isActive() ) {
					tx.rollback();
				}
				throw e;
			}
		}
		catch (Exception e) {
			if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
				tm.rollback();
			}
			throw e;
		}
	}
}
