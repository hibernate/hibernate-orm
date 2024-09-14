/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;

/**
 * Parameter descriptor specific to mutations.  It exposes metadata about the parameter
 *
 * @author Steve Ebersole
 */
public class ColumnValueParameter extends AbstractJdbcParameter {
	private final ColumnReference columnReference;
	private final ParameterUsage usage;

	public ColumnValueParameter(ColumnReference columnReference, ParameterUsage usage) {
		super( columnReference.getJdbcMapping() );
		this.columnReference = columnReference;
		this.usage = usage;
	}

	@Override
	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public ParameterUsage getUsage() {
		return usage;
	}

	@Override
	public String toString() {
		return "ColumnValueParameter(" + columnReference.getColumnExpression() + ')';
	}
}
