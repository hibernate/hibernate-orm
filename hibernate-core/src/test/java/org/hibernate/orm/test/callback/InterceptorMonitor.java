/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class InterceptorMonitor {
	private static boolean instantiated;
	private static final AtomicInteger persistCount = new AtomicInteger();

	public InterceptorMonitor() {
		instantiated = true;
	}

	public void entityPersisted() {
		persistCount.incrementAndGet();
	}

	public static boolean wasInstantiated() {
		return instantiated;
	}

	public static int currentPersistCount() {
		return persistCount.get();
	}

	public static void reset() {
		instantiated = false;
		persistCount.set( 0 );
	}

	public static class Resetter implements BeforeEachCallback {
		@Override
		public void beforeEach(ExtensionContext context) {
			InterceptorMonitor.reset();
			CdiTrackingInterceptor.clearInstances();
		}
	}
}
