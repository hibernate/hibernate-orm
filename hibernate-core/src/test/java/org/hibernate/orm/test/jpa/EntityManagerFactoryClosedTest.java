/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.util.Map;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * EntityManagerFactoryClosedTest
 *
 * @author Scott Marlow
 */
public class EntityManagerFactoryClosedTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare(options);
		options.put( AvailableSettings.JAKARTA_TRANSACTION_TYPE, "JTA" );
	}

	/**
	 * Test that using a closed EntityManagerFactory throws an IllegalStateException
	 * Also ensure that HHH-8586 doesn't regress.
	 * @throws Exception
	 */
	@Test
	public void testWithTransactionalEnvironment() throws Exception {

		assertFalse( JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()) );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		assertTrue( JtaStatusHelper.isActive(TestingJtaPlatformImpl.INSTANCE.getTransactionManager()) );
		EntityManagerFactory entityManagerFactory = entityManagerFactory();

		entityManagerFactory.close();	// close the underlying entity manager factory

		try {
			entityManagerFactory.createEntityManager();
			fail( "expected IllegalStateException when calling emf.createEntityManager with closed emf" );
		} catch( IllegalStateException expected ) {
			// success

		}

		try {
			entityManagerFactory.getCriteriaBuilder();
			fail( "expected IllegalStateException when calling emf.getCriteriaBuilder with closed emf" );
		} catch( IllegalStateException expected ) {
			// success
		}

		try {
			entityManagerFactory.getCache();
			fail( "expected IllegalStateException when calling emf.getCache with closed emf" );
		} catch( IllegalStateException expected ) {
			// success
		}

		assertFalse( entityManagerFactory.isOpen() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

}
