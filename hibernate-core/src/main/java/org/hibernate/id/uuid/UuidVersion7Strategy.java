/*
 * SPDX-License-Identifier: Apache-2.0
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
 *     <li>62 bits - pseudorandom counter to guarantee additional monotonicity, uniqueness, and good entropy.</li>
 * </ul>
 *
 * @author Cedomir Igaly
 * @apiNote Version 7 features a time-ordered value field derived from the widely implemented and
 * well-known Unix Epoch timestamp source, the number of milliseconds since midnight 1 Jan 1970 UTC,
 * leap seconds excluded.
 */
public class UuidVersion7Strategy implements UUIDGenerationStrategy, UuidValueGenerator {

	private static final long MAX_RANDOM_SEQUENCE = 0x3FFF_FFFF_FFFF_FFFFL;

	public static final UuidVersion7Strategy INSTANCE = new UuidVersion7Strategy();

	@Internal
	public static class Holder {
		private static final SecureRandom numberGenerator = new SecureRandom();

	}

	public record State(Instant lastTimestamp, long lastSequence, long nanos) {

		State(Instant lastTimestamp, long lastSequence) {
			this( lastTimestamp, lastSequence, nanos( lastTimestamp ) );
		}

		public long millis() {
			return lastTimestamp.toEpochMilli();
		}

		private static long nanos(Instant timestamp) {
			return (long) ((timestamp.getNano() % 1_000_000L) * 0.004096);
		}

		public State getNextState() {
			final Instant now = Instant.now();
			if ( lastTimestamp.toEpochMilli() < now.toEpochMilli() ||
				lastTimestamp.toEpochMilli() == now.toEpochMilli() && nanos < nanos( now ) ) {
				return new State( now, randomSequence() );
			}
			final long nextSequence = lastSequence + Holder.numberGenerator.nextLong( 0xFFFF_FFFFL );
			if ( nextSequence > MAX_RANDOM_SEQUENCE ) {
				return new State( lastTimestamp.plusNanos( 250 ), randomSequence() );
			}
			else {
				return new State( lastTimestamp, nextSequence );
			}
		}

		private static long randomSequence() {
			return Holder.numberGenerator.nextLong( MAX_RANDOM_SEQUENCE );
		}
	}

	private final AtomicReference<State> lastState;

	@Internal
	public UuidVersion7Strategy() {
		this( Instant.EPOCH, Long.MIN_VALUE );
	}

	@Internal
	public UuidVersion7Strategy(final Instant initialTimestamp, final long initialSequence) {
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
				// LSB bits 2-15 - pseudorandom counter
				| state.lastSequence
		);
	}
}
