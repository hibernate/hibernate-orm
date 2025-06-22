/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SqlAliasStemHelper {
	/**
	 * Singleton access
	 */
	public static final SqlAliasStemHelper INSTANCE = new SqlAliasStemHelper();

	public String generateStemFromEntityName(String entityName) {
		return acronym( toSimpleEntityName( entityName ) );
	}

	private String toSimpleEntityName(String entityName) {
		String simpleName = StringHelper.unqualify( entityName );
		if ( simpleName.contains( "$" ) ) {
			// inner class
			simpleName = simpleName.substring( simpleName.lastIndexOf( '$' ) + 1 );
		}
		if ( StringHelper.isEmpty( simpleName ) ) {
			throw new AssertionFailure( "Could not determine simple name as base for alias [" + entityName + "]" );
		}
		return simpleName;
	}

	public String generateStemFromAttributeName(String attributeName) {
		return acronym(attributeName);
	}


	private String acronym(String name) {
		StringBuilder string = new StringBuilder();
		char last = '\0';
		for (int i = 0; i<name.length(); i++ ) {
			char ch = name.charAt(i);
			if ( Character.isLetter(ch) ) {
				if ( string.length() == 0
						|| Character.isUpperCase(ch) && !Character.isUpperCase(last) ) {
					string.append( Character.toLowerCase(ch) );
				}
			}
			last = ch;
		}
		return string.length() == 0 ? "z" : string.toString();
	}
}
