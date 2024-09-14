/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

/**
 * A Runnable used to test ClassLoaderLeakDetector
 * @see ClassLoaderLeaksUtilityTest
 */
public class NotLeakingTestAction implements Runnable {

	@Override
	public void run() {
		checkExpectedClassLoader( getClass() );
	}

	protected void checkExpectedClassLoader(Class aClass) {
		final ClassLoader owningClassloader = aClass.getClassLoader();
		if ( !owningClassloader.getName().equals( "TestIsolatedIsolatedClassLoader" ) ) {
			throw new IllegalStateException( "Not being loaded by the expected classloader" );
		}
	}

}
