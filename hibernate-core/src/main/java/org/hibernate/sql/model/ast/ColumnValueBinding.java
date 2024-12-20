/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.Objects;

import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * Represents the binding of a value to a column.  Can be used to
 * uniformly model value assignments wrt inserts, updates and upserts.
 *
 * @apiNote Practically speaking, the {@linkplain #getValueExpression() value}
 * can only be a JDBC parameter or a literal.
 *
 * @author Steve Ebersole
 */
public class ColumnValueBinding {
	private final ColumnReference columnReference;
	private final ColumnWriteFragment valueExpression;

	public ColumnValueBinding(ColumnReference columnReference, ColumnWriteFragment valueExpression) {
		this.columnReference = columnReference;
		this.valueExpression = valueExpression;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public ColumnWriteFragment getValueExpression() {
		return valueExpression;
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
