/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Steve Ebersole
 */
public class Monitor {
	private static final AtomicInteger count = new AtomicInteger( 0 );
	private static boolean instantiated;

	public Monitor() {
		instantiated = true;
	}

	public static void reset() {
		instantiated = false;
		count.set( 0 );
	}

	public static boolean wasInstantiated() {
		return instantiated;
	}

	public static int currentCount() {
		return count.get();
	}

	public void entitySaved() {
		count.getAndIncrement();
	}

	public static class Resetter implements BeforeEachCallback {
		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			Monitor.reset();
		}
	}
}
