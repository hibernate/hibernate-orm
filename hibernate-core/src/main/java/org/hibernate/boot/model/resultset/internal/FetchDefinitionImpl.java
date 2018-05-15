/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.resultset.internal;

import org.hibernate.LockMode;
import org.hibernate.boot.model.resultset.spi.ResultSetMappingDefinition;

/**
 * Models a defined Fetch (Hibernate only)
 */
public class FetchDefinitionImpl implements ResultSetMappingDefinition.Fetch {
	private final String tableAlias;
	private final String parentTableAlias;
	private final String fetchedRoleName;
	private final LockMode lockMode;

	public FetchDefinitionImpl(
			String tableAlias,
			String parentTableAlias,
			String fetchedRoleName,
			LockMode lockMode) {
		this.tableAlias = tableAlias;
		this.parentTableAlias = parentTableAlias;
		this.fetchedRoleName = fetchedRoleName;
		this.lockMode = lockMode;
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public String getParentTableAlias() {
		return parentTableAlias;
	}

	@Override
	public String getFetchedRoleName() {
		return fetchedRoleName;
	}
}
