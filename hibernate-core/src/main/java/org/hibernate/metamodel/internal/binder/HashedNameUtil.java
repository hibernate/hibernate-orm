/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.binder;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * @author Brett Meyer
 */
public class HashedNameUtil {

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 * Static so the name can be generated prior to creating the Constraint.
	 * They're cached, keyed by name, in multiple locations.
	 * 
	 * @param prefix
	 * @param tableName
	 * @param columnNames
	 * @return String The generated name
	 */
	public static String generateName(String prefix, String tableName, String... columnNames ) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.

		StringBuilder sb = new StringBuilder( "table`" + tableName + "`" );

		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		String[] alphabeticalColumns = columnNames.clone();
		Arrays.sort( alphabeticalColumns );
		for ( String columnName : alphabeticalColumns ) {
			sb.append( "column`" + columnName + "`" );
		}
		return prefix + hashedName( sb.toString() );
	}

	/**
	 * Helper method for {@link #generateName}
	 * 
	 * @param prefix
	 * @param tableName
	 * @param columnNames
	 *
	 * @return String The generated name
	 */
	public static String generateName(String prefix, String tableName, List<String> columnNames) {
		String[] columnNamesArray = new String[columnNames.size()];
		for ( int i = 0; i < columnNames.size(); i++ ) {
			columnNamesArray[i] = columnNames.get( i );
		}
		return generateName( prefix, tableName, columnNamesArray );
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 * 
	 * @param s The name to be hashed.
	 * @return String The hased name.
	 */
	public static String hashedName(String s) {
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
