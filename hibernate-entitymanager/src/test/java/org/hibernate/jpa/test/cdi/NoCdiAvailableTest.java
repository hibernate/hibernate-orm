/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.cdi;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.ByteCodeHelper;

import org.junit.Rule;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ClassLoadingIsolater;
import org.hibernate.testing.junit4.ExtraAssertions;

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
					return new ClassLoader(NoCdiAvailableTest.class.getClassLoader()) {
						@Override
						public Class<?> loadClass(String name) throws ClassNotFoundException {
							for ( String excludedPackage : EXCLUDED_PACKAGES ) {
								if ( name.startsWith( excludedPackage ) ) {
									throw new CdiClassLoadException( "CDI classes [" + name + "] excluded from load" );
								}
							}

							Class c = findLoadedClass( name );
							if ( c != null ) {
								return c;
							}

							final String resourceName = name.replace( '.', '/' ) + ".class";
							final URL resource = getResource( resourceName );
							if ( resource == null ) {
								throw new ClassNotFoundException( name + " not found" );
							}
							if ( !"file".equals( resource.getProtocol() ) ) {
								return getParent().loadClass( name );
							}

							InputStream is = this.getResourceAsStream( name.replace( '.', '/' ) + ".class" );
							if ( is == null ) {
								throw new ClassNotFoundException( name + " not found" );
							}

							try {
								byte[] bytecode = ByteCodeHelper.readByteCode( is );
								return defineClass( name, bytecode, 0, bytecode.length );
							}
							catch( Throwable t ) {
								throw new ClassNotFoundException( "Error reading class file for " + name, t );
							}
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
		mainMethod.invoke( null );
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
