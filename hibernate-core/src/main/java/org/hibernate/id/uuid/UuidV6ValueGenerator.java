/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

/**
 * Implements a <a href="https://datatracker.ietf.org/doc/html/rfc9562#name-uuid-version-6">UUID Version 6</a> generation strategy as defined by the {@link UUID#randomUUID()} method.
 *
 * @author Cedomir Igaly
 */
public class UuidV6ValueGenerator implements UUIDGenerationStrategy, UuidValueGenerator {

	private static final Instant EPOCH_1582;

	static {
		EPOCH_1582 = LocalDate.of( 1582, 10, 15 )
				.atStartOfDay( ZoneId.of( "UTC" ) )
				.toInstant();
	}

	private static class Holder {

		static final SecureRandom numberGenerator = new SecureRandom();
	}

	public static final UuidV6ValueGenerator INSTANCE = new UuidV6ValueGenerator();

	private final Lock lock = new ReentrantLock( true );

	private long currentTimestamp;

	private final AtomicLong clockSequence = new AtomicLong( 0 );

	public UuidV6ValueGenerator() {
		this( getCurrentTimestamp(), 0 );
	}

	public UuidV6ValueGenerator(final long currentTimestamp, final long clockSequence) {
		this.currentTimestamp = currentTimestamp;
		this.clockSequence.set( clockSequence );
	}

	/**
	 * A variant 6
	 */
	@Override
	public int getGeneratedVersion() {
		// UUIDv6 is a field-compatible version of UUIDv1, reordered for improved DB locality
		return 6;
	}

	/**
	 * Delegates to {@link UUID#randomUUID()}
	 */
	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return generateUuid( session );
	}


	/**
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
	 * @param session session
	 *
	 * @return UUID version 6
	 * @see UuidValueGenerator#generateUuid(SharedSessionContractImplementor)
	 */
	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		final long currentTimestamp = getCurrentTimestamp();

		return new UUID(
				currentTimestamp << 4 & 0xFFFF_FFFF_FFFF_0000L
				| 0x6000L
				| currentTimestamp & 0x0FFFL,
				0x8000_0000_0000_0000L
				| ( getSequence( currentTimestamp ) & 0x3FFFL ) << 48
				| Holder.numberGenerator.nextLong() & 0xFFFF_FFFF_FFFFL
		);
	}


	private long getSequence(final long currentTimestamp) {
		lock.lock();
		try {
			if ( this.currentTimestamp > currentTimestamp ) {
				this.currentTimestamp = currentTimestamp;
				clockSequence.set( 0 );
			}
		}
		finally {
			lock.unlock();
		}
		return clockSequence.getAndIncrement();
	}

	private static long getCurrentTimestamp() {
		final Duration duration = Duration.between( EPOCH_1582, Instant.now() );
		return duration.toSeconds() * 10_000_000 + duration.toNanosPart() / 100;
	}
}
