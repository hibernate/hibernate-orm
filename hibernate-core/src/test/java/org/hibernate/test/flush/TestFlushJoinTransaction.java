/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import java.util.Map;

import javax.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

/**
 * @author Michiel Hendriks
 */
@TestForIssue(jiraKey = "HHH-13936")
public class TestFlushJoinTransaction extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
	}
	
	@Test
	public void testFlush() throws Exception {
		Session session = openSession();
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			session.flush();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (TransactionRequiredException e) {
			Assert.fail("No TransactionRequiredException expected.");
		}
		finally {
			session.close();
		}
	}
	
	@Test
	public void testIsConnectedFlush() throws Exception {
		Session session = openSession();
		try {
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			session.isConnected();
			session.flush();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (TransactionRequiredException e) {
			Assert.fail("No TransactionRequiredException expected.");
		}
		finally {
			session.close();
		}
	}
}
