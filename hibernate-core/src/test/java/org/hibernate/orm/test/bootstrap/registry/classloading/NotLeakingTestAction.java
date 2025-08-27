/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
