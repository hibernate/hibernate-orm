/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.persistence.EntityManagerFactory;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ClassLoadingIsolater;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Test JPA bootstrapping when CDI is not available for classloading.
 *
 * @author Steve Ebersole
 */
public class NoCdiAvailableTest extends BaseUnitTestCase {
	public static final String[] EXCLUDED_PACKAGES = new String[] {
			"javax.enterprise.inject.",
			"javax.enterprise.context."
	};

	private static class CdiClassLoadException extends RuntimeException {
		private CdiClassLoadException(String message) {
			super( message );
		}
	}

	@Rule public ClassLoadingIsolater isolater = new ClassLoadingIsolater(
			new ClassLoadingIsolater.IsolatedClassLoaderProvider() {

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
			}
	);

	@Test
	public void testJpaBootstrapWithoutCdiAvailable() throws Exception {
		Class delegateClass = Thread.currentThread().getContextClassLoader().loadClass(
				"org.hibernate.jpa.test.cdi.NoCdiAvailableTestDelegate"
		);
		Method mainMethod = delegateClass.getMethod( "passingNoBeanManager" );
		EntityManagerFactory entityManagerFactory = null;
		try {
			entityManagerFactory = (EntityManagerFactory) mainMethod.invoke( null );
		}
		finally {
			if (entityManagerFactory != null ) {
				entityManagerFactory.close();
			}
		}
	}

	@Test
	public void testJpaBootstrapWithoutCdiAvailablePassingCdi() throws Throwable {
		Class delegateClass = Thread.currentThread().getContextClassLoader().loadClass(
				"org.hibernate.jpa.test.cdi.NoCdiAvailableTestDelegate"
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
