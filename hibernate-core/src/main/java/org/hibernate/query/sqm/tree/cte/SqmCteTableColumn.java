/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class SqmCteTableColumn {
	private final SqmCteTable cteTable;
	private final String columnName;
	private final BasicType typeExpressable;
	private final boolean allowNulls;

	public SqmCteTableColumn(
			SqmCteTable cteTable,
			String columnName,
			BasicType typeExpressable,
			boolean allowNulls) {
		this.cteTable = cteTable;
		this.columnName = columnName;
		this.typeExpressable = typeExpressable;
		this.allowNulls = allowNulls;
	}

	public SqmCteTable getCteTable() {
		return cteTable;
	}

	public String getColumnName() {
		return columnName;
	}

	public BasicType getType() {
		return typeExpressable;
	}

	public boolean isAllowNulls() {
		return allowNulls;
	}
}
