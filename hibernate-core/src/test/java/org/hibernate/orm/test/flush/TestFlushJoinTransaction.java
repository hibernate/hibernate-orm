/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import java.util.Map;

import jakarta.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static org.junit.Assert.fail;

/**
 * @author Michiel Hendriks
 */
@JiraKey(value = "HHH-13936")
public class TestFlushJoinTransaction extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map<String,Object> settings) {
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
			fail("No TransactionRequiredException expected.");
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
			fail("No TransactionRequiredException expected.");
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testIsConnectedFlushShouldThrowExceptionIfNoTransaction() {
		Session session = openSession();
		try {
			session.isConnected();
			session.flush();
			fail("A TransactionRequiredException should be thrown");
		}
		catch (TransactionRequiredException e) {
			//expected
		}
		finally {
			session.close();
		}
	}
}
