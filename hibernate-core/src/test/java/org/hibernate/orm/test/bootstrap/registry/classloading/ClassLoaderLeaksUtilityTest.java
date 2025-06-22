/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * Verifies basic operations of {@link ClassLoaderLeakDetector}.
 */
public class ClassLoaderLeaksUtilityTest {

	@Test
	public void testClassLoaderLeaksDetected() {
		//N.B. since we expect to timeout in this case, reduce the timeouts to not require
		//a significant amount of time during each ORM test run.
		Assert.assertFalse( ClassLoaderLeakDetector.verifyActionNotLeakingClassloader( "org.hibernate.orm.test.bootstrap.registry.classloading.LeakingTestAction", 2 , 2 ) );
	}

	@Test
	public void testClassLoaderLeaksNegated() {
		Assert.assertTrue( ClassLoaderLeakDetector.verifyActionNotLeakingClassloader( "org.hibernate.orm.test.bootstrap.registry.classloading.NotLeakingTestAction" ) );
	}

}
