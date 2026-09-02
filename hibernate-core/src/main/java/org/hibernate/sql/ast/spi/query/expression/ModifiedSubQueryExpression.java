/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class ModifiedSubQueryExpression implements Expression {
	public enum Modifier {
		ALL( "all" ),
		ANY( "any" ),
		SOME( "some" );

		private final String sqlName;

		Modifier(String sqlName) {
			this.sqlName = sqlName;
		}

		public String getSqlName() {
			return sqlName;
		}
	}

	private final SelectStatement subQuery;
	private final Modifier modifier;

	public ModifiedSubQueryExpression(SelectStatement subQuery, Modifier modifier) {
		this.subQuery = subQuery;
		this.modifier = modifier;
	}

	public SelectStatement getSubQuery() {
		return subQuery;
	}

	public Modifier getModifier() {
		return modifier;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitModifiedSubQueryExpression( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return subQuery.getExpressionType();
	}
}
