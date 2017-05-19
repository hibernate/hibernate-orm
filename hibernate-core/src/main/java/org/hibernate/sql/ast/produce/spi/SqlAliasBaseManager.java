/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.sql.ast.produce.metamodel.spi.NavigableReferenceInfo;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseResolver;

/**
 * Helper used in creating unique SQL table aliases for a SQL AST
 *
 * @author Steve Ebersole
 */
public class SqlAliasBaseManager implements SqlAliasBaseResolver {
	// an overall dictionary.  used to ensure that a given
	// 		NavigableReferenceInfo instance always resolves to the same
	// 		SQL alias base; its called base because a NavigableReferenceInfo
	// 		can encompass multiple physical tables - each needing its own SQL
	// 		alias which we derive from the base.
	private Map<NavigableReferenceInfo,SqlAliasBase> fromElementAliasMap = new HashMap<>();

	// work dictionary used to map an acronym to the number of times it has
	// 		been used.
	private Map<String,Integer> acronymCountMap = new HashMap<>();

	public SqlAliasBase getSqlAliasBase(NavigableReferenceInfo navigableReferenceInfo) {
		return fromElementAliasMap.computeIfAbsent(
				navigableReferenceInfo,
				e -> generateAliasBase( navigableReferenceInfo )
		);
	}

	private SqlAliasBase generateAliasBase(NavigableReferenceInfo domainReference) {
		final String acronym = domainReference.getReferencedNavigable().getSqlAliasStem();

		Integer acronymCount = acronymCountMap.get( acronym );
		if ( acronymCount == null ) {
			acronymCount = 0;
		}
		acronymCount++;
		acronymCountMap.put( acronym, acronymCount );

		return new SqlAliasBaseImpl( acronym + acronymCount );
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
