/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.io.Serializable;

import org.hibernate.metamodel.mapping.ValueMapping;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTableColumn implements Serializable {
	private final SqmCteTable cteTable;
	private final String columnName;
	private final ValueMapping typeExpressible;

	public SqmCteTableColumn(
			SqmCteTable cteTable,
			String columnName,
			ValueMapping typeExpressible) {
		this.cteTable = cteTable;
		this.columnName = columnName;
		this.typeExpressible = typeExpressible;
	}

	public SqmCteTable getCteTable() {
		return cteTable;
	}

	public String getColumnName() {
		return columnName;
	}

	public ValueMapping getType() {
		return typeExpressible;
	}

}
