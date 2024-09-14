/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class FromClauseAccessImpl implements FromClauseAccess {
	private Map<String, TableGroup> tableGroupBySqlAlias;
	private Map<NavigablePath, TableGroup> tableGroupByPath;

	public FromClauseAccessImpl() {
	}

	public TableGroup getByAlias(String alias) {
		final TableGroup byAlias = findByAlias( alias );
		if ( byAlias == null ) {
			throw new IllegalArgumentException( "Could not resolve TableGroup by alias [" + alias + "]" );
		}
		return byAlias;
	}

	public TableGroup findByAlias(String alias) {
		if ( tableGroupBySqlAlias != null ) {
			return tableGroupBySqlAlias.get( alias );
		}

		return null;
	}

	@Override
	public TableGroup findTableGroupOnCurrentFromClause(NavigablePath navigablePath) {
		return null;
	}

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		if ( tableGroupByPath != null ) {
			final TableGroup tableGroup = tableGroupByPath.get( navigablePath );
			return tableGroup;
		}

		return null;
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		if ( tableGroupByPath == null ) {
			tableGroupByPath = new HashMap<>();
		}
		tableGroupByPath.put( navigablePath, tableGroup );

		if ( tableGroup.getGroupAlias() != null ) {
			if ( tableGroupBySqlAlias == null ) {
				tableGroupBySqlAlias = new HashMap<>();
			}
			tableGroupBySqlAlias.put( tableGroup.getGroupAlias(), tableGroup );
		}
	}
}
