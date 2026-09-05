/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTypedExpression;

/**
 * Models a column's value expression within the SQL AST. Used to model:<ul>
 *     <li>a column's new value (UPDATE SET clause or INSERT VALUES clause)</li>
 *     <li>a column's old value in a restriction (optimistic locking, etc.)</li>
 * </ul>
 *
 * @see ColumnTransformer#write()
 *
 * @author Steve Ebersole
 */
public class ColumnWriteFragment implements SqlTypedExpression {
	private final String fragment;
	private final List<ColumnValueParameter> parameters;
	private final SelectableMapping selectableMapping;
	private final Expression expression;

	public ColumnWriteFragment(String fragment, SelectableMapping selectableMapping) {
		this( fragment, Collections.emptyList(), selectableMapping );
	}

	public ColumnWriteFragment(String fragment, ColumnValueParameter parameter, SelectableMapping selectableMapping) {
		this( fragment, Collections.singletonList( parameter ), selectableMapping );
		assert !fragment.contains( "?" ) || parameter != null;
	}

	public ColumnWriteFragment(String fragment, List<ColumnValueParameter> parameters, SelectableMapping selectableMapping) {
		this.fragment = fragment;
		this.parameters = parameters;
		this.selectableMapping = selectableMapping;
		this.expression = null;
	}

	public ColumnWriteFragment(
			List<ColumnValueParameter> parameters,
			SelectableMapping selectableMapping,
			Expression expression) {
		this.fragment = null;
		this.parameters = parameters;
		this.selectableMapping = selectableMapping;
		this.expression = expression;
	}

	public String getFragment() {
		return fragment;
	}

	public Collection<ColumnValueParameter> getParameters() {
		return parameters;
	}

	@Override
	public SqlTypedMapping getSqlTypedMapping() {
		return selectableMapping;
	}

	@Override
	public JdbcMapping getExpressionType() {
		return selectableMapping.getJdbcMapping();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		if ( expression == null ) {
			sqlTreeWalker.visitColumnWriteFragment( this );
		}
		else {
			expression.accept( sqlTreeWalker );
		}
	}

	@Override
	public String toString() {
		final Object renderedExpression = expression == null ? fragment : expression;
		return switch ( parameters.size() ) {
			case 0 -> String.format(
					Locale.ROOT,
					"ColumnWriteFragment(%s)@%s",
					renderedExpression,
					hashCode()
			);
			case 1 -> String.format(
					Locale.ROOT,
					"ColumnWriteFragment(%s = %s (%s))@%s",
					parameters.get( 0 ).getColumnReference().getColumnExpression(),
					renderedExpression,
					parameters.get( 0 ).getUsage(),
					hashCode()
			);
			default -> String.format(
					Locale.ROOT,
					"ColumnWriteFragment(%s = %s (%s))@%s",
					parameters,
					renderedExpression,
					parameters.get( 0 ).getUsage(),
					hashCode()
			);
		};
	}
}
