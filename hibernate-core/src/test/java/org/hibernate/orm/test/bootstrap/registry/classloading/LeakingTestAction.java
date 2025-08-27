/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

/**
 * This runnable will intentionally leak the owning classloader:
 * useful to test our leak detection utilities.
 * @see ClassLoaderLeaksUtilityTest
 */
public final class LeakingTestAction extends NotLeakingTestAction {

	private final ThreadLocal tl = new ThreadLocal();

	@Override
	public void run() {
		super.run();
		tl.set( this );
	}

}
