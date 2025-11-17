/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static java.util.Comparator.comparing;

/**
 * @author Steve Ebersole
 */
public class NamingHelper {
	/**
	 * Singleton access
	 */
	public static final NamingHelper INSTANCE = new NamingHelper();

	public static NamingHelper withCharset(String charset) {
		return new NamingHelper( charset );
	}

	private final String charset;

	public NamingHelper() {
		this(null);
	}

	private NamingHelper(String charset) {
		this.charset = charset;
	}

	/**
	 * If a foreign-key is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 */
	public String generateHashedFkName(
			String prefix,
			Identifier tableName,
			Identifier referencedTableName,
			List<Identifier> columnNames) {
		return generateHashedFkName(
				prefix,
				tableName,
				referencedTableName,
				columnNames == null || columnNames.isEmpty()
						? new Identifier[0]
						: columnNames.toArray( new Identifier[0] )
		);
	}

	/**
	 * If a foreign-key is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 */
	public String generateHashedFkName(
			String prefix,
			Identifier tableName,
			Identifier referencedTableName,
			Identifier... columnNames) {
		// Use a concatenation that guarantees uniqueness, even if identical
		// names exist between all table and column identifiers.
		final StringBuilder text = new StringBuilder()
				.append( "table`" ).append( tableName ).append( "`" )
				.append( "references`" ).append( referencedTableName ).append( "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final Identifier[] alphabeticalColumns = columnNames.clone();
		Arrays.sort( alphabeticalColumns, comparing( Identifier::getCanonicalName ) );
		for ( Identifier columnName : alphabeticalColumns ) {
			assert columnName != null;
			text.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( text.toString() );
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 *
	 * @return String The generated name
	 */
	public String generateHashedConstraintName(
			String prefix, Identifier tableName, Identifier... columnNames ) {
		// Use a concatenation that guarantees uniqueness, even if identical
		// names exist between all table and column identifiers.
		final StringBuilder text = new StringBuilder( "table`" + tableName + "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final Identifier[] alphabeticalColumns = columnNames.clone();
		Arrays.sort( alphabeticalColumns, comparing(Identifier::getCanonicalName) );
		for ( Identifier columnName : alphabeticalColumns ) {
			assert columnName != null;
			text.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( text.toString() );
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 *
	 * @return String The generated name
	 */
	public String generateHashedConstraintName(
			String prefix, Identifier tableName, List<Identifier> columnNames) {
		final Identifier[] columnNamesArray = new Identifier[columnNames.size()];
		for ( int i = 0; i < columnNames.size(); i++ ) {
			columnNamesArray[i] = columnNames.get( i );
		}
		return generateHashedConstraintName( prefix, tableName, columnNamesArray );
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing that the length of the name will
	 * always be smaller than the 30 character identifier restriction
	 * enforced by some dialects.
	 *
	 * @param name The name to be hashed.
	 *
	 * @return String The hashed name.
	 */
	public String hashedName(String name) {
		final byte[] bytes;
		try {
			bytes = charset == null
					? name.getBytes()
					: name.getBytes( charset );
		}
		catch (UnsupportedEncodingException uee) {
			throw new IllegalArgumentException(uee);
		}
		final byte[] digest = hash( pad( bytes ) );
		return new BigInteger( 1, digest ).toString( 35 );
	}

	// Constants for MD5
	private static final int[] S = {
			7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
			5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
			4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
			6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21
	};

	private static final int[] K = new int[64];
	static {
		for ( int i = 0; i < 64; i++ ) {
			K[i] = (int)(long) ( (1L << 32) * Math.abs( Math.sin( i + 1 ) ) );
		}
	}

	public static byte[] hash(byte[] message) {
		int a0 = 0x67452301;
		int b0 = 0xefcdab89;
		int c0 = 0x98badcfe;
		int d0 = 0x10325476;

		for ( int i = 0; i < message.length / 64; i++ ) {
			final int[] M = new int[16];
			for (int j = 0; j < 16; j++) {
				M[j] = ((message[i * 64 + j * 4] & 0xFF))
					| ((message[i * 64 + j * 4 + 1] & 0xFF) << 8)
					| ((message[i * 64 + j * 4 + 2] & 0xFF) << 16)
					| ((message[i * 64 + j * 4 + 3] & 0xFF) << 24);
			}

			int A = a0, B = b0, C = c0, D = d0;

			for (int j = 0; j < 64; j++) {
				final int F, g;
				if (j < 16) {
					F = (B & C) | (~B & D);
					g = j;
				}
				else if (j < 32) {
					F = (D & B) | (~D & C);
					g = (5 * j + 1) % 16;
				}
				else if (j < 48) {
					F = B ^ C ^ D;
					g = (3 * j + 5) % 16;
				}
				else {
					F = C ^ (B | ~D);
					g = (7 * j) % 16;
				}

				final int temp = D;
				D = C;
				C = B;
				B = B + Integer.rotateLeft( A + F + K[j] + M[g], S[j] );
				A = temp;
			}

			a0 += A;
			b0 += B;
			c0 += C;
			d0 += D;
		}

		// Convert final state to byte array (little-endian)
		final byte[] digest = new byte[16];
		encodeInt( digest, 0, a0 );
		encodeInt( digest, 4, b0 );
		encodeInt( digest, 8, c0 );
		encodeInt( digest, 12, d0 );
		return digest;
	}

	private static void encodeInt(byte[] output, int offset, int value) {
		output[offset]     = (byte) (value & 0xFF);
		output[offset + 1] = (byte) ((value >>> 8) & 0xFF);
		output[offset + 2] = (byte) ((value >>> 16) & 0xFF);
		output[offset + 3] = (byte) ((value >>> 24) & 0xFF);
	}

	private static byte[] pad(byte[] input) {
		final int originalLength = input.length;
		final int numPaddingBytes = ( 56 - (originalLength + 1) % 64 + 64 ) % 64;

		final byte[] padded = new byte[originalLength + 1 + numPaddingBytes + 8];
		System.arraycopy( input, 0, padded, 0, originalLength );
		padded[originalLength] = (byte) 0x80;

		long bitLength = (long) originalLength * 8;
		for ( int i = 0; i < 8; i++ ) {
			padded[padded.length - 8 + i] = (byte) ( ( bitLength >>> (8 * i) ) & 0xFF );
		}

		return padded;
	}
}
