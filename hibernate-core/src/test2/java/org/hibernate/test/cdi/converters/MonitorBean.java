/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.converters;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A CDI Bean used to verify injection into an AttributeConverter
 *
 * @author Steve Ebersole
 */
public class MonitorBean {
	private static boolean instantiated;
	private static final AtomicInteger toDbCount = new AtomicInteger( 0 );
	private static final AtomicInteger fromDbCount = new AtomicInteger( 0 );

	public MonitorBean() {
		instantiated = true;
	}

	public static void reset() {
		instantiated = false;
		toDbCount.set( 0 );
		fromDbCount.set( 0 );
	}

	public static boolean wasInstantiated() {
		return instantiated;
	}

	public static int currentToDbCount() {
		return toDbCount.get();
	}

	public static int currentFromDbCount() {
		return fromDbCount.get();
	}

	public void toDbCalled() {
		toDbCount.getAndIncrement();
	}

	public void fromDbCalled() {
		fromDbCount.getAndIncrement();
	}
}
