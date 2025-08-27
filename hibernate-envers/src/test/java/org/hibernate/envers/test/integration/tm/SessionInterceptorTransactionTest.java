/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.tm;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.transaction.TransactionManager;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.junit.Test;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

/**
 * @author Chris Cranford
 */
public class SessionInterceptorTransactionTest extends BaseEnversJPAFunctionalTestCase {

	private TransactionManager tm;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );

		TestInterceptor.reset();

		options.put( AvailableSettings.SESSION_SCOPED_INTERCEPTOR, TestInterceptor.class.getName() );
		options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );

		TestingJtaBootstrap.prepare( options );
		tm = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
	}

	@Test
	@Priority(10)
	public void initData() throws Exception {
		// Revision 1
		EntityManager em = getEntityManager();
		// Explicitly use manual flush to trigger separate temporary session write via Envers
		em.unwrap( Session.class ).setHibernateFlushMode( FlushMode.MANUAL );
		tm.begin();
		StrTestEntity entity = new StrTestEntity( "Test" );
		em.persist( entity );
		em.flush();
		tm.commit();
	}

	@Test
	public void testInterceptorInvocations() throws Exception {
		// The interceptor should only be created once and should only be invoked once.
		final Map<TestInterceptor, Integer> invocationMap = TestInterceptor.getBeforeCompletionCallbacks();
		assertEquals( 1, invocationMap.size() );
		assertEquals( invocationMap.values().stream().filter( v -> v == 1 ).count(), 1 );
	}
}
