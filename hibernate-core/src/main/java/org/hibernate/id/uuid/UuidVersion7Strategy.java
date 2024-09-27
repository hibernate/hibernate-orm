/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Implements UUID Version 7 generation strategy as defined by the <a href="https://datatracker.ietf.org/doc/html/rfc9562#name-uuid-version-7">RFC 9562</a>.
 *
 * <ul>
 *     <li>48 bits - 48-bit big-endian unsigned number of the Unix Epoch timestamp in milliseconds.</li>
 *     <li>4 bits - version field, set to 0b0111 (7).</li>
 *     <li>
 *         12 bits - sub-milliseconds part of timestamp (resolution approximately 1/4 of millisecond)
 *         to guarantee additional monotonicity.
 *     </li>
 *     <li>2 bits - variant field, set to 0b10.</li>
 *     <li>14 bits - counter to guarantee additional monotonicity, resets to 0 when timestamp changes. </li>
 *     <li>48 bits - pseudorandom data to provide uniqueness.</li>
 * </ul>
 *
 * @author Cedomir Igaly
 * @apiNote Version 7 features a time-ordered value field derived from the widely implemented and
 * well-known Unix Epoch timestamp source, the number of milliseconds since midnight 1 Jan 1970 UTC,
 * leap seconds excluded.
 */
public class UuidVersion7Strategy implements UUIDGenerationStrategy, UuidValueGenerator {

	public static final UuidVersion7Strategy INSTANCE = new UuidVersion7Strategy();

	private static class Holder {
		private static final SecureRandom numberGenerator = new SecureRandom();

	}

	public record State(Instant timestamp, int sequence) {
		public long millis() {
			return timestamp.toEpochMilli();
		}

		public long nanos() {
			return (long) ( ( timestamp.getNano() % 1_000_000L ) * 0.004096 );
		}

		public State getNextState() {
			final Instant now = Instant.now();
			if ( timestamp.toEpochMilli() < now.toEpochMilli() ) {
				return new State(
						now.truncatedTo( MILLIS ),
						randomSequence()
				);
			}
			else if ( sequence == 0x3FFF ) {
				return new State(
						timestamp.plusMillis( 1 ),
						Holder.numberGenerator.nextInt( 1 << 14 )
				);
			}
			else {
				return new State( timestamp, sequence + 1 );
			}
		}

		private static int randomSequence() {
			return Holder.numberGenerator.nextInt( 1 << 14 );
		}
	}

	private final AtomicReference<State> lastState;

	@Internal
	public UuidVersion7Strategy() {
		this( Instant.EPOCH, Integer.MIN_VALUE );
	}

	@Internal
	public UuidVersion7Strategy(final Instant initialTimestamp, final int initialSequence) {
		this.lastState = new AtomicReference<>( new State( initialTimestamp, initialSequence ) );
	}

	/**
	 * Version 7
	 */
	@Override
	public int getGeneratedVersion() {
		return 7;
	}

	@Override
	public UUID generateUUID(final SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	@Override
	public UUID generateUuid(final SharedSessionContractImplementor session) {
		final State state = lastState.updateAndGet( State::getNextState );

		return new UUID(
				// MSB bits 0-47 - 48-bit big-endian unsigned number of the Unix Epoch timestamp in milliseconds
				state.millis() << 16 & 0xFFFF_FFFF_FFFF_0000L
				// MSB bits 48-51 - version = 7
				| 0x7000L
				// MSB bits 52-63 - sub-milliseconds part of timestamp
				| state.nanos() & 0xFFFL,
				// LSB bits 0-1 - variant = 4
				0x8000_0000_0000_0000L
				// LSB bits 2-15 - counter
				| ( state.sequence & 0x3FFFL ) << 48
				// LSB bits 16-63 - pseudorandom data
				| randomNode()
		);
	}

	private static long randomNode() {
		return Holder.numberGenerator.nextLong( 0x1_0000_0000_0000L ) | 0x1000_0000_0000L;
	}
}
