/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events;

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
}
