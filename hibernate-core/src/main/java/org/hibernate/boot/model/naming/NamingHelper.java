/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class NamingHelper {
	/**
	 * Singleton access
	 */
	public static final NamingHelper INSTANCE = new NamingHelper();

	/**
	 * If a foreign-key is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 */
	public String generateHashedFkName(
			String prefix,
			Identifier tableName,
			Identifier referencedTableName,
			List<Identifier> columnNames) {
		final Identifier[] columnNamesArray;
		if ( columnNames == null || columnNames.isEmpty() ) {
			columnNamesArray = new Identifier[0];
		}
		else {
			columnNamesArray = columnNames.toArray( new Identifier[ columnNames.size() ] );
		}

		return generateHashedFkName(
				prefix,
				tableName,
				referencedTableName,
				columnNamesArray
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
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.

		StringBuilder sb = new StringBuilder()
				.append( "table`" ).append( tableName ).append( "`" )
				.append( "references`" ).append( referencedTableName ).append( "`" );

		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		Identifier[] alphabeticalColumns = columnNames.clone();
		Arrays.sort(
				alphabeticalColumns,
				new Comparator<Identifier>() {
					@Override
					public int compare(Identifier o1, Identifier o2) {
						return o1.getCanonicalName().compareTo( o2.getCanonicalName() );
					}
				}
		);

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
	public String generateHashedConstraintName(String prefix, Identifier tableName, Identifier... columnNames ) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.

		StringBuilder sb = new StringBuilder( "table`" + tableName + "`" );

		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		Identifier[] alphabeticalColumns = columnNames.clone();
		Arrays.sort(
				alphabeticalColumns,
				new Comparator<Identifier>() {
					@Override
					public int compare(Identifier o1, Identifier o2) {
						return o1.getCanonicalName().compareTo( o2.getCanonicalName() );
					}
				}
		);
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
	public String generateHashedConstraintName(String prefix, Identifier tableName, List<Identifier> columnNames) {
		Identifier[] columnNamesArray = new Identifier[columnNames.size()];
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
	 * @param s The name to be hashed.
	 *
	 * @return String The hashed name.
	 */
	public String hashedName(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			md.update( s.getBytes() );
			byte[] digest = md.digest();
			BigInteger bigInt = new BigInteger( 1, digest );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch ( NoSuchAlgorithmException e ) {
			throw new HibernateException( "Unable to generate a hashed name!", e );
		}
	}
}
