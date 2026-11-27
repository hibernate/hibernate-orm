package org.hibernate.orm.test.cache;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.spi.support.SimpleTimestamper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Tests that {@link SimpleTimestamper} does not wait for the system clock.
 */
public class SimpleTimestamperTest {

	@Test
	public void testIncreasingValues() {
		long previous = SimpleTimestamper.next();
		for ( int i = 0; i < 10_000; i++ ) {
			long next = SimpleTimestamper.next();
			assertThat( next ).isGreaterThan( previous );
			previous = next;
		}
	}

	@Test
	public void testSystemClockMovedBackwards() throws Exception {
		final Field field = SimpleTimestamper.class.getDeclaredField( "VALUE" );
		field.setAccessible( true );
		final AtomicLong value = (AtomicLong) field.get( null );
		final long original = value.get();
		try {
			// simulate a timestamp issued one day ahead of the current system clock
			final long future = ( System.currentTimeMillis() + Duration.ofDays( 1 ).toMillis() ) * SimpleTimestamper.ONE_MS;
			value.set( future );

			final long first = assertTimeoutPreemptively( Duration.ofSeconds( 10 ), SimpleTimestamper::next );
			final long second = assertTimeoutPreemptively( Duration.ofSeconds( 10 ), SimpleTimestamper::next );

			assertThat( first ).isGreaterThan( future );
			assertThat( second ).isGreaterThan( first );
		}
		finally {
			value.set( original );
		}
	}
}
