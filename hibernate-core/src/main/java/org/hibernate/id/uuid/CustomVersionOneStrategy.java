/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.internal.util.BytesHelper;

import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import static org.hibernate.id.uuid.Helper.getAddressBytes;
import static org.hibernate.id.uuid.Helper.getCountBytes;
import static org.hibernate.id.uuid.Helper.getJvmIdentifierBytes;

/**
 * Applies a version 1 (time-based) generation strategy (using ip address rather than mac address) but applies them in a
 * different layout.  The strategy is very similar to the legacy {@link org.hibernate.id.UUIDHexGenerator} id generator
 * but uses a RFC 4122 compliant layout (variant 2).
 *
 * @implNote Can be a bottleneck due to the need to synchronize in order to increment an internal count as part of the
 *           algorithm.
 *
 * @author Steve Ebersole
 */
public class CustomVersionOneStrategy implements UUIDGenerationStrategy, UuidValueGenerator {
	@Override
	public int getGeneratedVersion() {
		return 1;
	}

	private final long mostSignificantBits;

	public CustomVersionOneStrategy() {
		// generate the "most significant bits" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final byte[] hiBits = new byte[8];
		// use address as the first 32 bits (8 * 4 bytes)
		arraycopy( getAddressBytes(), 0, hiBits, 0, 4 );
		// use the "jvm identifier" as the next 32 bits
		arraycopy( getJvmIdentifierBytes(), 0, hiBits, 4, 4 );
		// set the version (rfc term) appropriately
		hiBits[6] &= 0x0f;
		hiBits[6] |= 0x10;
		mostSignificantBits = BytesHelper.asLong( hiBits );
	}

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		return new UUID( mostSignificantBits,
				generateLeastSignificantBits( currentTimeMillis() ) );
	}

	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	public long getMostSignificantBits() {
		return mostSignificantBits;
	}

	public static long generateLeastSignificantBits(long seed) {
		final byte[] loBits = new byte[8];
		final short hiTime = (short) ( seed >>> 32 );
		final int loTime = (int) seed;
		arraycopy( BytesHelper.fromShort( hiTime ), 0, loBits, 0, 2 );
		arraycopy( BytesHelper.fromInt( loTime ), 0, loBits, 2, 4 );
		arraycopy( getCountBytes(), 0, loBits, 6, 2 );
		loBits[0] &= 0x3f;
		loBits[0] |= ((byte)2 << (byte)6);
		return BytesHelper.asLong( loBits );
	}

	@AllowSysOut
	public static void main(String[] args) {
		final var strategy = new CustomVersionOneStrategy();

		for ( int i = 0; i < 1000; i++ ) {
			System.out.println( "Generation # " + i + " ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			byte[] loBits = new byte[8];

			long sysTime = currentTimeMillis();
			short hiTime = (short) ( currentTimeMillis() >>> 32 );
			int loTime = (int) sysTime;
			arraycopy( BytesHelper.fromShort( hiTime ), 0, loBits, 0, 2 );
			arraycopy( BytesHelper.fromInt( loTime ), 0, loBits, 2, 4 );
			arraycopy( getCountBytes(), 0, loBits, 6, 2 );

			System.out.println( "    before bit setting ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			System.out.println( "       loBits[0] : " + BytesHelper.toBinaryString( loBits[0] ) );
			System.out.println( "             lsb : " + BytesHelper.toBinaryString( BytesHelper.asLong( loBits ) ) );
			System.out.println( "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

			loBits[0] &= 0x3f;
			loBits[0] |= ((byte)2 << (byte)6);

			System.out.println( "    after bit setting ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			System.out.println( "       loBits[0] : " + BytesHelper.toBinaryString( loBits[0] ) );
			long leastSignificantBits = BytesHelper.asLong( loBits );
			System.out.println( "             lsb : " + BytesHelper.toBinaryString( leastSignificantBits ) );
			System.out.println( "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );


			UUID uuid = new UUID( strategy.mostSignificantBits, leastSignificantBits );
			System.out.println( "  uuid : " + uuid );
			System.out.println( "  variant : " + uuid.variant() );
			System.out.println( "  version : " + uuid.version() );
			if ( uuid.variant() != 2 ) {
				throw new RuntimeException( "bad variant" );
			}
			System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		}
	}
}
