/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Verifies that Hibernate ORM won't leak the classloader.
 * We only test in H2 to save some time; we also need to avoid
 * leaks by JDBC drivers, and since these aren't our responsibility
 * it's best to focus on a single DB.
 */
@RequiresDialect(H2Dialect.class)
@Disabled
public class HibernateClassLoaderLeaksTest {

	private static Set<Driver> knownDrivers;

	@BeforeAll
	public static void prepareForClassLoaderLeakTest() {
		final String property = System.getProperty( "log4j2.disableJmx" );
		Assert.assertEquals( "To be able to test against leaks, the system property 'log4j2.disableJmx' must be set to true",
							"true", property );

		//Attempt to workaround the mess of DriverManager leaks by clearing it before the test;
		//it will most certainly re-register all drivers again within the test running context,
		//but that will imply that the isolated classloader will also have permission to de-register them.
		knownDrivers = DriverManager.drivers().collect( Collectors.toUnmodifiableSet() );
		knownDrivers.forEach( HibernateClassLoaderLeaksTest::cleanup );
	}

	@AfterAll
	public static void restoreRegisteredDrivers() throws SQLException {
		if ( knownDrivers != null ) {
			for ( Driver driver : knownDrivers ) {
				DriverManager.registerDriver( driver );
			}
		}
	}

	@Test
	public void hibernateDoesNotLeakClassloader() {
		ClassLoaderLeakDetector.assertNotLeakingAction( HibernateLoadingTestAction.class.getName() );
	}

	@Test
	public void hibernateDoesNotLeakClassloaderWithCallbacks() {
		ClassLoaderLeakDetector.assertNotLeakingAction( HibernateCallbacksTestAction.class.getName() );
	}

	private static void cleanup(Driver driver) {
		System.out.println( "Attempting de-registration of driver: " + driver );
		try {
			DriverManager.deregisterDriver( driver );
		}
		catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

}
