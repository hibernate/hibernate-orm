/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCacheable;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTableColumn implements JpaCteCriteriaAttribute, SqmCacheable {
	private final SqmCteTable<?> cteTable;
	private final String columnName;
	private final SqmBindableType<?> typeExpressible;

	public SqmCteTableColumn(
			SqmCteTable<?> cteTable,
			String columnName,
			SqmBindableType<?> typeExpressible) {
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
	public @Nullable Class<?> getJavaType() {
		return typeExpressible == null ? null : typeExpressible.getJavaType();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		return o instanceof SqmCteTableColumn that
			&& columnName.equals( that.columnName )
			&& typeExpressible.equals( that.typeExpressible );
	}

	@Override
	public int hashCode() {
		int result = columnName.hashCode();
		result = 31 * result + typeExpressible.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object o) {
		return equals( o );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
