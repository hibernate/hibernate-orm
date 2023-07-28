/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
		Assert.assertFalse( ClassLoaderLeakDetector.verifyActionNotLeakingClassloader( "org.hibernate.orm.test.bootstrap.registry.classloading.LeakingTestAction" ) );
	}

	@Test
	public void testClassLoaderLeaksNegated() {
		Assert.assertTrue( ClassLoaderLeakDetector.verifyActionNotLeakingClassloader( "org.hibernate.orm.test.bootstrap.registry.classloading.NotLeakingTestAction" ) );
	}

}


