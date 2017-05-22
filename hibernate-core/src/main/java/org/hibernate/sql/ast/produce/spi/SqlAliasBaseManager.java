/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;

/**
 * Helper used in creating unique SQL table aliases for a SQL AST
 *
 * @author Steve Ebersole
 */
public class SqlAliasBaseManager implements SqlAliasBaseGenerator {
	// work dictionary used to map an acronym to the number of times it has
	// 		been used.
	private Map<String,Integer> acronymCountMap = new HashMap<>();

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

	private static class SqlAliasBaseImpl implements SqlAliasBase {
		private final String stem;
		private int aliasCount;

		SqlAliasBaseImpl(String stem) {
			this.stem = stem;
		}

		@Override
		public String getAliasStem() {
			return stem;
		}

		@Override
		public String generateNewAlias() {
			synchronized ( this ) {
				return stem + '_' + (aliasCount++);
			}
		}
	}
}
