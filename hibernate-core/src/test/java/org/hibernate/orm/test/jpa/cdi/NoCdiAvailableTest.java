/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cdi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.testing.orm.junit.ClassLoadingIsolaterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test JPA bootstrapping when CDI is not available for classloading.
 *
 * @author Steve Ebersole
 */
@ExtendWith(ClassLoadingIsolaterExtension.class)
public class NoCdiAvailableTest implements ClassLoadingIsolaterExtension.IsolatedClassLoaderProvider {
	public static final String[] EXCLUDED_PACKAGES = new String[] {
			"jakarta.enterprise.inject.",
			"jakarta.enterprise.context."
	};

	@Override
	public ClassLoader buildIsolatedClassLoader() {
		return new ClassLoader( NoCdiAvailableTest.class.getClassLoader() ) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				for ( String excludedPackage : EXCLUDED_PACKAGES ) {
					if ( name.startsWith( excludedPackage ) ) {
						throw new CdiClassLoadException( "CDI classes [" + name + "] excluded from load" );
					}
				}
				return super.loadClass( name );
			}
		};
	}

	@Override
	public void releaseIsolatedClassLoader(ClassLoader isolatedClassLoader) {
		// nothing to do

	}

	private static class CdiClassLoadException extends RuntimeException {
		private CdiClassLoadException(String message) {
			super( message );
		}
	}

	@Test
	public void testJpaBootstrapWithoutCdiAvailable() throws Exception {
		Class delegateClass = Thread.currentThread().getContextClassLoader().loadClass(
				"org.hibernate.orm.test.jpa.cdi.NoCdiAvailableTestDelegate"
		);
		Method mainMethod = delegateClass.getMethod( "passingNoBeanManager" );
		EntityManagerFactory entityManagerFactory = null;
		try {
			entityManagerFactory = (EntityManagerFactory) mainMethod.invoke( null );
		}
		finally {
			if ( entityManagerFactory != null ) {
				entityManagerFactory.close();
			}
		}
	}

	@Test
	public void testJpaBootstrapWithoutCdiAvailablePassingCdi() throws Throwable {
		Class delegateClass = Thread.currentThread().getContextClassLoader().loadClass(
				"org.hibernate.orm.test.jpa.cdi.NoCdiAvailableTestDelegate"
		);
		Method mainMethod = delegateClass.getMethod( "passingBeanManager" );
		try {
			mainMethod.invoke( null );
			fail( "Expecting failure from missing CDI classes" );
		}
		catch (InvocationTargetException expected) {
			// hard to assert specific exception types due to classloader trickery
		}
	}
}
