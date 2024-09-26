/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
 * @apiNote This strategy is field-compatible with Version 1, with the time bits reordered for improved DB locality.
 *
 * @author Cedomir Igaly
 */
public class UuidVersion6Strategy implements UUIDGenerationStrategy, UuidValueGenerator {
	public static final UuidVersion6Strategy INSTANCE = new UuidVersion6Strategy();

	private static class Holder {
		static final SecureRandom numberGenerator = new SecureRandom();
		static final Instant EPOCH_1582 = LocalDate.of( 1582, 10, 15 )
				.atStartOfDay( ZoneId.of( "UTC" ) )
				.toInstant();
	}

	private final Lock lock = new ReentrantLock( true );
	private final AtomicLong clockSequence = new AtomicLong( 0 );
	private long currentTimestamp;


	@Internal
	public UuidVersion6Strategy() {
		this( getCurrentTimestamp(), 0 );
	}

	@Internal
	public UuidVersion6Strategy(final long currentTimestamp, final long clockSequence) {
		this.currentTimestamp = currentTimestamp;
		this.clockSequence.set( clockSequence );
	}

	/**
	 * Version 6
	 */
	@Override
	public int getGeneratedVersion() {
		return 6;
	}

	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		final long currentTimestamp = getCurrentTimestamp();

		return new UUID(
				// MSB bits 0-47 - most significant 32 bits of the 60-bit starting timestamp
				currentTimestamp << 4 & 0xFFFF_FFFF_FFFF_0000L
				// MSB bits 48-51 - version = 6
				| 0x6000L
				// MSB bits 52-63 - least significant 12 bits from the 60-bit starting timestamp
				| currentTimestamp & 0x0FFFL,
				// LSB bits 0-1 - variant = 4
				0x8000_0000_0000_0000L
				// LSB bits 2-15 - clock sequence
				| ( getSequence( currentTimestamp ) & 0x3FFFL ) << 48
				// LSB bits 16-63 - pseudorandom data
				| Holder.numberGenerator.nextLong() & 0xFFFF_FFFF_FFFFL
		);
	}


	private long getSequence(final long currentTimestamp) {
		lock.lock();
		try {
			if ( this.currentTimestamp < currentTimestamp ) {
				this.currentTimestamp = currentTimestamp;
				clockSequence.updateAndGet( l -> l & 0x1FFFL );
			}
		}
		finally {
			lock.unlock();
		}
		return clockSequence.getAndIncrement();
	}

	private static long getCurrentTimestamp() {
		final Duration duration = Duration.between( Holder.EPOCH_1582, Instant.now() );
		return duration.toSeconds() * 10_000_000 + duration.toNanosPart() / 100;
	}
}
