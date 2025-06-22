/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import jakarta.persistence.EntityManagerFactory;

/**
 * A Runnable which initializes an EntityManagerFactory;
 * this is meant to test against classloader leaks, so needs
 * to be packaged as a Runnable rather than using our usual
 * testing facilities.
 * @see HibernateClassLoaderLeaksTest
 */
public class HibernateLoadingTestAction extends NotLeakingTestAction implements Runnable {

	@Override
	public final void run() {
		super.run(); //for basic sanity self-check
		final Map config = new HashMap();
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ) {
					@Override
					public boolean isExcludeUnlistedClasses() {
						return true;
					}

					@Override
					public List<String> getManagedClassNames() {
						return HibernateLoadingTestAction.this.getManagedClassNames();
					}
				},
				config
		).build();
		try {
			checkExpectedClassLoader( emf.unwrap( SessionFactory.class ).getClass() );
			actionOnHibernate( emf );
		}
		finally {
			try {
				emf.close();
			}
			finally {
				cleanupJDBCDrivers();
			}
		}
	}

	protected void actionOnHibernate(EntityManagerFactory emf) {
		//no-op
	}

	protected List<String> getManagedClassNames() {
		return Collections.emptyList();
	}

	private void cleanupJDBCDrivers() {
		DriverManager.drivers().forEach( this::deregister );
	}

	private void deregister(final Driver driver) {
		System.out.println( "Unregistering driver: " +driver);
		try {
			DriverManager.deregisterDriver( driver );
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

}
