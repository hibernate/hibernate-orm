/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;

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
		if ( table == null ) {
			table = defaultTable;
		}
		final TableReference tableReference = tableGroup.getTableReference( null, table, true );
		return tableReference == null ? null : tableReference.getIdentificationVariable();
	}

}
