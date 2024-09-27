/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

/**
 * Implements UUID Version 6 generation strategy as defined by the <a href="https://datatracker.ietf.org/doc/html/rfc9562#name-uuid-version-6">RFC 9562</a>.
 *
 * <ul>
 *     <li>32 bits - the most significant 32 bits of the 60-bit starting timestamp.</li>
 *     <li>16 bits - the middle 16 bits of the 60-bit starting timestamp.</li>
 *     <li>4 bits - version field, set to 0b0110 (6).</li>
 *     <li>12 bits - the least significant 12 bits from the 60-bit starting timestamp.</li>
 *     <li>2 bits - variant field, set to 0b10.</li>
 *     <li>14 bits - the clock sequence, resets to 0 when timestamp changes. </li>
 *     <li>48 bits - pseudorandom data to provide uniqueness.</li>
 * </ul>
 *
 * @author Cedomir Igaly
 * @apiNote This strategy is field-compatible with Version 1, with the time bits reordered for improved DB locality.
 */
public class UuidVersion6Strategy implements UUIDGenerationStrategy, UuidValueGenerator {
	public static final UuidVersion6Strategy INSTANCE = new UuidVersion6Strategy();

	private static class Holder {
		static final SecureRandom numberGenerator = new SecureRandom();
		static final long EPOCH_1582_SECONDS = LocalDate.of( 1582, 10, 15 )
				.atStartOfDay( ZoneId.of( "UTC" ) )
				.toInstant().getEpochSecond();

	}

	private record State(long timestamp, int sequence) {
		public State getNextState() {
			final long now = instantToTimestamp();
			if ( this.timestamp < now ) {
				return new State(
						now,
						randomSequence()
				);
			}
			else if ( sequence == 0x3FFF ) {
				return new State(
						this.timestamp + 1,
						randomSequence()
				);
			}
			else {
				return new State( timestamp, sequence + 1 );
			}
		}

		private static int randomSequence() {
			return Holder.numberGenerator.nextInt( 1 << 14 );
		}

		private static long instantToTimestamp() {
			final Instant instant = Instant.now();
			final long seconds = instant.getEpochSecond() - Holder.EPOCH_1582_SECONDS;
			return seconds * 10_000_000 + instant.getNano() / 100;
		}
	}

	private final AtomicReference<State> lastState;

	@Internal
	public UuidVersion6Strategy() {
		this( Long.MIN_VALUE, Integer.MIN_VALUE );
	}

	@Internal
	public UuidVersion6Strategy(final long initialTimestamp, final int initialSequence) {
		this.lastState = new AtomicReference<>( new State( initialTimestamp, initialSequence ) );
	}

	/**
	 * Version 6
	 */
	@Override
	public int getGeneratedVersion() {
		return 6;
	}

	@Override
	public UUID generateUUID(final SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	@Override
	public UUID generateUuid(final SharedSessionContractImplementor session) {
		final State state = lastState.updateAndGet( State::getNextState );

		return new UUID(
				// MSB bits 0-47 - most significant 32 bits of the 60-bit starting timestamp
				state.timestamp << 4 & 0xFFFF_FFFF_FFFF_0000L
				// MSB bits 48-51 - version = 6
				| 0x6000L
				// MSB bits 52-63 - least significant 12 bits from the 60-bit starting timestamp
				| state.timestamp & 0x0FFFL,
				// LSB bits 0-1 - variant = 4
				0x8000_0000_0000_0000L
				// LSB bits 2-15 - clock sequence
				| ( state.sequence & 0x3FFFL ) << 48
				// LSB bits 16-63 - pseudorandom data, least significant bit of the first octet is set to 1
				| randomNode()
		);
	}

	private static long randomNode() {
		return Holder.numberGenerator.nextLong( 0x1_0000_0000_0000L ) | 0x1000_0000_0000L;
	}
}
