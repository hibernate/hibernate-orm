/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.aggregate.MySQLAggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MySQL json_array function.
 */
public class MySQLJsonArrayFunction extends JsonArrayFunction {

	public MySQLJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "json_array()" );
		}
		else {
			final SqlAstNode lastArgument = sqlAstArguments.get( sqlAstArguments.size() - 1 );
			final JsonNullBehavior nullBehavior;
			final int argumentsCount;
			if ( lastArgument instanceof JsonNullBehavior jsonNullBehavior ) {
				nullBehavior = jsonNullBehavior;
				argumentsCount = sqlAstArguments.size() - 1;
			}
			else {
				nullBehavior = null;
				argumentsCount = sqlAstArguments.size();
			}
			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				// MySQL does not support retaining the order of arguments when using json_arrayagg,
				// so we have to use manual array appending instead
				sqlAppender.appendSql( '(' );
				for ( int i = argumentsCount - 1; i > 0; i-- ) {
					sqlAppender.appendSql( "select case when t.v is null then x.v else json_array_append(x.v,'$',");
					appendJsonWriteExpression( sqlAppender, sqlAstArguments.get( i ), () -> sqlAppender.appendSql( "t.v" ), walker );
					sqlAppender.appendSql( ") end v from (select " );
					sqlAstArguments.get( i ).accept( walker );
					sqlAppender.appendSql( " v) t,(" );
				}
				sqlAppender.appendSql( "select case when t.v is null then json_array() else json_array(");
				appendJsonWriteExpression( sqlAppender, sqlAstArguments.get( 0 ), () -> sqlAppender.appendSql( "t.v" ), walker );
				sqlAppender.appendSql( ") end v from (select " );
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.appendSql( " v) t" );
				for ( int i = 1; i < argumentsCount; i++ ) {
					sqlAppender.appendSql( ") x" );
				}
				sqlAppender.appendSql( ')' );
			}
			else {
				sqlAppender.appendSql( "json_array" );
				char separator = '(';
				for ( int i = 0; i < argumentsCount; i++ ) {
					sqlAppender.appendSql( separator );
					renderValue( sqlAppender, sqlAstArguments.get( i ), walker );
					separator = ',';
				}
				sqlAppender.appendSql( ')' );
			}
		}
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		appendJsonWriteExpression( sqlAppender, value, () -> value.accept( walker ), walker );
	}

	private void appendJsonWriteExpression(SqlAppender sqlAppender, SqlAstNode value, Runnable renderFunction, SqlAstTranslator<?> walker) {
		final JdbcMappingContainer expressionType = ((Expression) value).getExpressionType();
		((MySQLAggregateSupport) walker.getSessionFactory().getJdbcServices().getDialect().getAggregateSupport())
				.appendJsonWriteExpression( sqlAppender, renderFunction, expressionType.getSingleJdbcMapping() );
	}
}
