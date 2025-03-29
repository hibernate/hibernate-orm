/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.transaction.RollbackException;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.integration.reventity.ExceptionListenerRevEntity;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Same as {@link org.hibernate.orm.test.envers.integration.reventity.ExceptionListener}, but in a JTA environment.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaExceptionListener extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, ExceptionListenerRevEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		TestingJtaBootstrap.prepare( options );
	}

	@Test(expected = RollbackException.class)
	@Priority(5) // must run before testDataNotPersisted()
	public void testTransactionRollback() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		try {
			EntityManager em = getEntityManager();

			// Trying to persist an entity - however the listener should throw an exception, so the entity
			// shouldn't be persisted
			StrTestEntity te = new StrTestEntity( "x" );
			em.persist( te );
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	@Test
	public void testDataNotPersisted() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		try {
			// Checking if the entity became persisted
			EntityManager em = getEntityManager();
			long count = em.createQuery( "from StrTestEntity s where s.str = 'x'" ).getResultList().size();
			Assert.assertEquals( 0, count );
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
		}
	}
}
