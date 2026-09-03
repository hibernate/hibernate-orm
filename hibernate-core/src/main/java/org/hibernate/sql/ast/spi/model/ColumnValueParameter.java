/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/// Parameter descriptor specific to mutations.
/// It exposes metadata about the parameter.
///
/// [!NOTE]
/// > Especially note the [usage][#getUsage] - we track parameters separately for
/// > [assignments][ParameterUsage#SET] and [restrictions][ParameterUsage#RESTRICT]
/// > to allow different values in each clause.  E.g.
/// > ````
/// update ...
/// set col = newValue
/// where col = oldValue
/// ````
///
/// The JDBC-parameter methods declared by this class are its provider-facing
/// contract. Provider code should invoke these methods directly and must not
/// depend on the internal superclass used to share implementation inside
/// Hibernate.
///
/// @author Steve Ebersole
public class ColumnValueParameter extends AbstractJdbcParameter {
	private final ColumnReference columnReference;
	private final ParameterUsage usage;

	public ColumnValueParameter(ColumnReference columnReference, ParameterUsage usage) {
		super( columnReference.getJdbcMapping() );
		this.columnReference = columnReference;
		this.usage = usage;
	}

	public ColumnValueParameter(ColumnReference columnReference) {
		this( columnReference, ParameterUsage.SET );
	}

	@Override
	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public ParameterUsage getUsage() {
		return usage;
	}

	/// The binder for this parameter.
	///
	/// @since 8.0
	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	/// The identifier of this parameter, which is always `null` for a column
	/// value parameter.
	///
	/// @since 8.0
	@Override
	public @Nullable Integer getParameterId() {
		return null;
	}

	/// The JDBC mapping of the referenced column.
	///
	/// @since 8.0
	@Override
	public JdbcMapping getJdbcMapping() {
		return columnReference.getJdbcMapping();
	}

	/// Accept the SQL AST walker as this parameter.
	///
	/// @since 8.0
	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitParameter( this );
	}

	@Override
	public String toString() {
		return "ColumnValueParameter(" + columnReference.getColumnExpression() + " : " + usage.name() + ")";
	}
}
