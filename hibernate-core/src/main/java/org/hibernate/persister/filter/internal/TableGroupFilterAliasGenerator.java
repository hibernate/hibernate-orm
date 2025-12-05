/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.filter.internal;

import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Rob Worsnop
 */
public class TableGroupFilterAliasGenerator implements FilterAliasGenerator {
	private final String defaultTable;
	private final TableGroup tableGroup;

	public TableGroupFilterAliasGenerator(String defaultTable, TableGroup tableGroup) {
		this.defaultTable = defaultTable;
		this.tableGroup = tableGroup;
	}

	@Override
	public String getAlias(String table) {
		final var tableReference =
				tableGroup.getTableReference( null, table == null ? defaultTable : table, true );
		return tableReference == null ? null : tableReference.getIdentificationVariable();
	}

}
