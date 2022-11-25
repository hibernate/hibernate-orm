/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast;

import java.util.Locale;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Models a column's "write fragment" within the SQL AST
 *
 * @see ColumnTransformer#write()
 *
 * @author Steve Ebersole
 */
public class ColumnWriteFragment implements Expression {
	private final String fragment;
	private final ColumnValueParameter parameter;
	private final JdbcMapping jdbcMapping;

	public ColumnWriteFragment(String fragment, ColumnValueParameter parameter, JdbcMapping jdbcMapping) {
		this.fragment = fragment;
		this.parameter = parameter;
		this.jdbcMapping = jdbcMapping;
	}

	public String getFragment() {
		return fragment;
	}

	public ColumnValueParameter getParameter() {
		return parameter;
	}

	@Override
	public JdbcMapping getExpressionType() {
		return jdbcMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitColumnWriteFragment( this );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"ColumnWriteFragment(%s = %s (%s))@%s",
				parameter.getColumnReference().getColumnExpression(),
				fragment,
				parameter.getUsage(),
				hashCode()
		);
	}
}
