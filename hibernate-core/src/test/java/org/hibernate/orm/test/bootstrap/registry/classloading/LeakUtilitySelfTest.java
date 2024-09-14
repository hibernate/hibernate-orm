/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the testing utility PhantomReferenceLeakDetector
 */
public class LeakUtilitySelfTest {

	@Test
	public void verifyLeakUtility() {
		PhantomReferenceLeakDetector.assertActionNotLeaking( LeakUtilitySelfTest::notALeak );
	}

	@Test
	public void verifyLeakUtilitySpotsLeak() {
		Assert.assertFalse( PhantomReferenceLeakDetector.verifyActionNotLeaking( LeakUtilitySelfTest::troubleSomeLeak, 2, 1 ) );
	}

	private static SomeSpecialObject notALeak() {
		return new SomeSpecialObject();
	}

	private static SomeSpecialObject troubleSomeLeak() {
		final SomeSpecialObject specialThing = new SomeSpecialObject();
		tl.set( specialThing );
		return specialThing;
	}

	private static final ThreadLocal tl = new ThreadLocal<>();

	static class SomeSpecialObject {
		@Override
		public String toString() {
			return "this is some hypothetical critical object";
		}
	}

}
