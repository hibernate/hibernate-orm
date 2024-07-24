package org.hibernate.orm.test.id.uuid.custom;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static java.lang.Math.round;
import static java.time.Instant.EPOCH;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.NANOS;

public class UUIDv7 {

	private static final Random RND = new SecureRandom();

	private static class State {

		private Duration currentTimestamp;
		private long clockSequence;

		private State(final TemporalUnit resolution) {
			currentTimestamp = getCurrentTimestamp( resolution );
			clockSequence = 0;
		}

		public State(final Duration currentTimestamp, final long clockSequence) {
			this.currentTimestamp = currentTimestamp;
			this.clockSequence = clockSequence;
		}

		public long getSequence(final Duration currentTimestamp) {
			if ( !this.currentTimestamp.equals( currentTimestamp ) ) {
				this.currentTimestamp = currentTimestamp;
				clockSequence = 0;
				return 0;
			}
			else {
				return clockSequence++;
			}
		}
	}

	private static final Map<TemporalUnit, State> state = Map.of(
			MILLIS, new State( MILLIS ),
			MICROS, new State( MICROS ),
			NANOS, new State( NANOS )
	);

	public static void initState(final TemporalUnit resolution, final Duration timestamp, final long sequence) {
		state.put( resolution, new State( timestamp, sequence ) );
	}

	private static Duration getCurrentTimestamp(final TemporalUnit resolution) {
		return Duration.between( EPOCH, Instant.now() ).truncatedTo( resolution );
	}

	public static UUID nextIdentifier() {
		return nextIdentifier( NANOS );
	}

	public static UUID nextIdentifier(final TemporalUnit resolution) {
		final var currentTimestamp = getCurrentTimestamp( resolution );

		final var seq = state.get( resolution ).getSequence( currentTimestamp );

		if ( resolution.equals( MILLIS ) ) {
			final var millis = ( currentTimestamp.getNano() / 1_000_000 ) / 1_000.;
			return new UUID(
					currentTimestamp.getSeconds() << 28 | (long) ( millis * 0xFFFL ) << 16 | 0x7000L | seq,
					0x8000_0000_0000_0000L | RND.nextLong()
			);
		}
		else if ( resolution.equals( MICROS ) ) {
			final var micros = ( currentTimestamp.getNano() / 1_000 ) / 1_000_000.;
			final var usecl = (long) ( micros * 0xFFF_FFFL );
			return new UUID(
					currentTimestamp.getSeconds() << 28 | usecl << 4 & 0xFFF_0000L | 0x7000L | usecl & 0xFFFL,
					0x8000_0000_0000_0000L | seq << 48 & 0x3FFF_0000_0000_0000L | RND.nextLong() & 0xFFFF_FFFF_FFFFL
			);
		}
		else if ( resolution.equals( NANOS ) ) {
			final var nanos = currentTimestamp.getNano() / 1_000_000_000.;
			final long nsec = (long) ( nanos * 0x3FFF_FFF_FFFL );
			return new UUID(
					currentTimestamp.getSeconds() << 28 | nsec >> 10 & 0xFFF_0000L | 0x7000L | nsec >> 14 & 0xFFFL,
					0x8000_0000_0000_0000L | nsec << 48 & 0x3FFF_0000_0000_0000L | seq << 40 & 0xFF00_0000_0000L | RND.nextLong() & 0xFF_FFFF_FFFFL
			);
		}
		throw new IllegalArgumentException();
	}

	public static long timestamp(final UUID uuid) {
		if ( uuid.version() != 7 ) {
			return uuid.timestamp();
		}
		return uuid.getMostSignificantBits() >> 28;
	}

	public static Instant instant(final UUID uuid) {
		final var msb = uuid.getMostSignificantBits();
		return Instant.ofEpochSecond( msb >> 28 )
				.plusNanos(
						round( ( uuid.getLeastSignificantBits() >> 16 & 0x3FFFL | msb << 14 & 0x3ff_c000L | msb << 10 & 0x3f_fc00_0000L )
							   / (double) 0x3f_ffff_ffffL * 1_000_000_000L ) );
	}
}
