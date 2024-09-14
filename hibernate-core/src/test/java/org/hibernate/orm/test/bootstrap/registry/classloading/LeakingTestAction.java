/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
