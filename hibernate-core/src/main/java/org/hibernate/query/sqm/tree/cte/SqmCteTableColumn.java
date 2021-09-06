/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.io.Serializable;

import org.hibernate.metamodel.mapping.ModelPart;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTableColumn implements Serializable {
	private final SqmCteTable cteTable;
	private final String columnName;
	private final ModelPart typeExpressable;

	public SqmCteTableColumn(
			SqmCteTable cteTable,
			String columnName,
			ModelPart typeExpressable) {
		this.cteTable = cteTable;
		this.columnName = columnName;
		this.typeExpressable = typeExpressable;
	}

	public SqmCteTable getCteTable() {
		return cteTable;
	}

	public String getColumnName() {
		return columnName;
	}

	public ModelPart getType() {
		return typeExpressable;
	}

}
