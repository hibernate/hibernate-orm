/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates increasing identifiers (in a single VM only). Not valid across multiple VMs.  Identifiers are not
 * necessarily strictly increasing, but usually are.
 * <p/>
 * Core while loop implemented by Alex Snaps - EHCache project - under ASL 2.0
 *
 * @author Hibernate team
 * @author Alex Snaps
 */
public final class Timestamper {
	private static final int BIN_DIGITS = 12;
	public static final short ONE_MS = 1 << BIN_DIGITS;
	private static final AtomicLong VALUE = new AtomicLong();

	public static long next() {
		while ( true ) {
			long base = System.currentTimeMillis() << BIN_DIGITS;
			long maxValue = base + ONE_MS - 1;

			for ( long current = VALUE.get(), update = Math.max( base, current + 1 ); update < maxValue;
					current = VALUE.get(), update = Math.max( base, current + 1 ) ) {
				if ( VALUE.compareAndSet( current, update ) ) {
					return update;
				}
			}
		}
	}

	private Timestamper() {
	}
}






