/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper used in creating unique SQL table aliases for a SQL AST
 *
 * @author Steve Ebersole
 */
public class SqlAliasBaseManager implements SqlAliasBaseGenerator {
	// work dictionary used to map an acronym to the number of times it has been used.
	private final Map<String, Integer> acronymCountMap;

	public SqlAliasBaseManager() {
		acronymCountMap = new HashMap<>();
	}

	public SqlAliasBaseManager(Set<String> usedAcronyms) {
		acronymCountMap = new HashMap<>( usedAcronyms.size() );
		for ( String acronym : usedAcronyms ) {
			// Everything after the last underscore is the table index
			final int underscoreIndex = acronym.lastIndexOf('_');
			for ( int i = underscoreIndex - 1; i >= 0; i-- ) {
				if ( !Character.isDigit( acronym.charAt( i ) ) ) {
					final String stem = acronym.substring( 0, i + 1 );
					final int acronymValue = Integer.parseInt( acronym.substring( i + 1, underscoreIndex ) );
					final Integer acronymCount = acronymCountMap.get( stem );
					if ( acronymCount == null || acronymCount < acronymValue ) {
						acronymCountMap.put( stem, acronymValue );
					}
					break;
				}
			}
		}
	}

	@Override
	public SqlAliasBase createSqlAliasBase(String stem) {
		Integer acronymCount = acronymCountMap.get( stem );
		if ( acronymCount == null ) {
			acronymCount = 0;
		}
		acronymCount++;
		acronymCountMap.put( stem, acronymCount );

		return new SqlAliasBaseImpl( stem + acronymCount );
	}

}
