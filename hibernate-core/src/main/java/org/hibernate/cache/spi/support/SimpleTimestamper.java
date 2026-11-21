package org.hibernate.cache.spi.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates increasing identifiers (in a single VM only). Not valid across multiple VMs.  Identifiers are not
 * necessarily strictly increasing, but usually are.
 * <p>
 * Core while loop implemented by Alex Snaps - EHCache project - under ASL 2.0
 *
 * @author Hibernate team
 * @author Alex Snaps
 */
public final class SimpleTimestamper {
	private static final int BIN_DIGITS = 12;
	private static final AtomicLong VALUE = new AtomicLong();

	public static final short ONE_MS = 1 << BIN_DIGITS;

	public static long next() {
		final long base = System.currentTimeMillis() << BIN_DIGITS;
		final long maxValue = base + ONE_MS - 1;
		while ( true ) {
			final long current = VALUE.get();
			final long update = Math.max( base, current + 1 );
			if ( update >= maxValue ) {
				// Either the sequence for the current millisecond is exhausted, or the system clock moved
				// backwards. Never wait for the clock, as that could take arbitrarily long; keep increasing instead.
				return VALUE.incrementAndGet();
			}
			if ( VALUE.compareAndSet( current, update ) ) {
				return update;
			}
		}
	}

	public static int timeOut() {
		return (int) TimeUnit.SECONDS.toMillis( 60 ) * ONE_MS;
	}

	private SimpleTimestamper() {
	}
}
