/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the testing utility PhantomReferenceLeakDetector
 */
@BaseUnitTest
public class LeakUtilitySelfTest {

	@Test
	public void verifyLeakUtility() {
		PhantomReferenceLeakDetector.assertActionNotLeaking( LeakUtilitySelfTest::notALeak );
	}

	@Test
	public void verifyLeakUtilitySpotsLeak() {
		assertThat( PhantomReferenceLeakDetector.verifyActionNotLeaking( LeakUtilitySelfTest::troubleSomeLeak, 2, 1 ) )
				.isFalse();
	}

	private static SomeSpecialObject notALeak() {
		return new SomeSpecialObject();
	}

	private static SomeSpecialObject troubleSomeLeak() {
		final SomeSpecialObject specialThing = new SomeSpecialObject();
		tl.set( specialThing );
		return specialThing;
	}

	private static final ThreadLocal<SomeSpecialObject> tl = new ThreadLocal<>();

	static class SomeSpecialObject {
		@Override
		public String toString() {
			return "this is some hypothetical critical object";
		}
	}

}
