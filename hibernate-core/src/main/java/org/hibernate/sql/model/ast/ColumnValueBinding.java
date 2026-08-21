/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.Objects;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * Represents the binding of a value to a column.  Uniformly
 * models both value assignments and value restrictions in
 * relation to inserts, updates and upserts.
 *
 * @author Steve Ebersole
 */
public class ColumnValueBinding {
	private final ColumnReference columnReference;
	private final ColumnWriteFragment valueExpression;
	private final boolean attributeInsertable;
	private final boolean attributeUpdatable;

	public ColumnValueBinding(ColumnReference columnReference, ColumnWriteFragment valueExpression) {
		this.columnReference = columnReference;
		this.valueExpression = valueExpression;
		this.attributeInsertable = determineAttributeInsertable( valueExpression );
		this.attributeUpdatable = determineAttributeUpdatable( valueExpression );
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public ColumnWriteFragment getValueExpression() {
		return valueExpression;
	}

	public boolean isAttributeInsertable() {
		return attributeInsertable;
	}

	public boolean isAttributeUpdatable() {
		return attributeUpdatable;
	}

	private static boolean determineAttributeInsertable(ColumnWriteFragment valueExpression) {
		return valueExpression == null
			|| !(valueExpression.getSqlTypedMapping() instanceof SelectableMapping selectableMapping)
			|| selectableMapping.isInsertable();
	}

	private static boolean determineAttributeUpdatable(ColumnWriteFragment valueExpression) {
		return valueExpression == null
			|| !(valueExpression.getSqlTypedMapping() instanceof SelectableMapping selectableMapping)
			|| selectableMapping.isUpdateable();
	}

	@Override
	public String toString() {
		return "ColumnValueBinding(" + valueExpression + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ColumnValueBinding that = (ColumnValueBinding) o;
		return Objects.equals( columnReference, that.columnReference );
	}

	@Override
	public int hashCode() {
		return Objects.hash( columnReference );
	}
}
