/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;

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
		final StringBuilder sb = new StringBuilder()
				.append( "table`" ).append( tableName ).append( "`" )
				.append( "references`" ).append( referencedTableName ).append( "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final Identifier[] alphabeticalColumns = columnNames.clone();
		Arrays.sort( alphabeticalColumns, comparing( Identifier::getCanonicalName ) );
		for ( Identifier columnName : alphabeticalColumns ) {
			sb.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( sb.toString() );
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
		final StringBuilder sb = new StringBuilder( "table`" + tableName + "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final Identifier[] alphabeticalColumns = columnNames.clone();
		Arrays.sort( alphabeticalColumns, comparing(Identifier::getCanonicalName) );
		for ( Identifier columnName : alphabeticalColumns ) {
			sb.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( sb.toString() );
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
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 *
	 * @param name The name to be hashed.
	 *
	 * @return String The hashed name.
	 */
	public String hashedName(String name) {
		try {
			final MessageDigest md5 = MessageDigest.getInstance( "MD5" );
			md5.reset();
			md5.update( charset != null ? name.getBytes( charset ) : name.getBytes() );
			final BigInteger bigInt = new BigInteger( 1, md5.digest() );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch ( NoSuchAlgorithmException | UnsupportedEncodingException e ) {
			throw new HibernateException( "Unable to generate a hashed name", e );
		}
	}
}
