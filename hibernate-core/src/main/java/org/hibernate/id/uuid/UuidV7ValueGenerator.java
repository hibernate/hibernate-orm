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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;

import static java.time.Instant.EPOCH;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Implements a <a href="https://datatracker.ietf.org/doc/html/rfc9562#name-uuid-version-7">UUID Version 7</a> generation strategy as defined by the {@link UUID#randomUUID()} method.
 *
 * @author Cedomir Igaly
 */
public class UuidV7ValueGenerator implements UUIDGenerationStrategy, UuidValueGenerator {

	public static final UuidV7ValueGenerator INSTANCE = new UuidV7ValueGenerator();

	private static class Holder {

		static final SecureRandom numberGenerator = new SecureRandom();
	}

	private final Lock lock = new ReentrantLock( true );

	private Duration currentTimestamp;

	private final AtomicLong clockSequence;

	public UuidV7ValueGenerator() {
		this( getCurrentTimestamp(), 0 );
	}

	public UuidV7ValueGenerator(final Duration currentTimestamp, final long clockSequence) {
		this.currentTimestamp = currentTimestamp;
		this.clockSequence = new AtomicLong( clockSequence );
	}

	/**
	 * A variant 7
	 */
	@Override
	public int getGeneratedVersion() {
		/*
		 *	UUIDv7 features a time-ordered value field derived from the widely implemented and well-
		 * known Unix Epoch timestamp source, the number of milliseconds since midnight 1 Jan 1970 UTC,
		 * leap seconds excluded.
		 */
		return 7;
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
	 * @param session session
	 *
	 * @return UUID version 7
	 * @see UuidValueGenerator#generateUuid(SharedSessionContractImplementor)
	 */
	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		final Duration currentTimestamp = getCurrentTimestamp();

		final long seq = getSequence( currentTimestamp );

		final long millis = currentTimestamp.getSeconds() * 1000 + currentTimestamp.getNano() / 1_000_000;
		final long nanosPart = Math.round( ( currentTimestamp.getNano() % 1_000_000L ) * 0.004096 );

		return new UUID(
				millis << 16 & 0xFFFF_FFFF_FFFF_0000L | 0x7000L | nanosPart & 0xFFFL,
				0x8000_0000_0000_0000L | ( seq & 0x3FFFL ) << 48 | Holder.numberGenerator.nextLong() & 0xFFFF_FFFF_FFFFL
		);
	}

	private long getSequence(final Duration currentTimestamp) {
		lock.lock();
		try {
			if ( !this.currentTimestamp.equals( currentTimestamp ) ) {
				this.currentTimestamp = currentTimestamp;
				clockSequence.set( 0 );
			}
		}
		finally {
			lock.unlock();
		}
		return clockSequence.getAndIncrement();
	}

	private static Duration getCurrentTimestamp() {
		return Duration.between( EPOCH, Instant.now() ).truncatedTo( MILLIS );
	}
}
