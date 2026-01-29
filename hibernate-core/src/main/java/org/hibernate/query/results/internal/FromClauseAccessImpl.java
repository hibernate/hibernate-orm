/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * FromClauseAccess implementation used while building
 * {@linkplain ResultSetMapping} references.
 *
 * @author Steve Ebersole
 */
public class FromClauseAccessImpl implements FromClauseAccess {
	private Map<String, TableGroup> tableGroupBySqlAlias;
	private Map<NavigablePath, TableGroup> tableGroupByPath;

	public TableGroup findByAlias(String alias) {
		return tableGroupBySqlAlias == null ? null : tableGroupBySqlAlias.get( alias );
	}

	@Override
	public @Nullable TableGroup findTableGroupByIdentificationVariable(String identificationVariable) {
		for ( var tableGroup : tableGroupByPath.values() ) {
			if ( tableGroup.findTableReference( identificationVariable ) != null ) {
				return tableGroup;
			}
		}
		return null;
	}

	@Override
	public TableGroup findTableGroupOnCurrentFromClause(NavigablePath navigablePath) {
		return null;
	}

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		return tableGroupByPath == null ? null : tableGroupByPath.get( navigablePath );

	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		if ( tableGroupByPath == null ) {
			tableGroupByPath = new HashMap<>();
		}
		tableGroupByPath.put( navigablePath, tableGroup );

		final String groupAlias = tableGroup.getGroupAlias();
		if ( groupAlias != null ) {
			if ( tableGroupBySqlAlias == null ) {
				tableGroupBySqlAlias = new HashMap<>();
			}
			tableGroupBySqlAlias.put( groupAlias, tableGroup );
		}
	}
}
