/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		final TableReference tableReference = tableGroup.getTableReference( null, table, true, true );
		return tableReference == null ? null : tableReference.getIdentificationVariable();
	}

}
