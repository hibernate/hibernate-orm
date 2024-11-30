/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTableColumn implements JpaCteCriteriaAttribute {
	private final SqmCteTable<?> cteTable;
	private final String columnName;
	private final SqmExpressible<?> typeExpressible;

	public SqmCteTableColumn(
			SqmCteTable<?> cteTable,
			String columnName,
			SqmExpressible<?> typeExpressible) {
		this.cteTable = cteTable;
		this.columnName = columnName;
		this.typeExpressible = typeExpressible;
	}

	public SqmCteTable<?> getCteTable() {
		return cteTable;
	}

	public String getColumnName() {
		return columnName;
	}

	public SqmExpressible<?> getType() {
		return typeExpressible;
	}

	@Override
	public JpaCteCriteriaType<?> getDeclaringType() {
		return cteTable;
	}

	@Override
	public String getName() {
		return columnName;
	}

	@Override
	public Class<?> getJavaType() {
		return typeExpressible == null ? null : typeExpressible.getBindableJavaType();
	}
}
