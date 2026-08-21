/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.aggregate.SQLServerAggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_array function.
 */
public class SQLServerJsonArrayFunction extends JsonArrayFunction {

	private final boolean supportsExtendedJson;

	public SQLServerJsonArrayFunction(boolean supportsExtendedJson, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.supportsExtendedJson = supportsExtendedJson;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( supportsExtendedJson ) {
			super.render( sqlAppender, sqlAstArguments, returnType, walker );
		}
		else {
			if ( sqlAstArguments.isEmpty() ) {
				sqlAppender.appendSql( "N'[]'" );
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
					nullBehavior = JsonNullBehavior.ABSENT;
					argumentsCount = sqlAstArguments.size();
				}
				if ( nullBehavior == JsonNullBehavior.ABSENT ) {
					sqlAppender.appendSql( "(select N'['+string_agg(substring(t.d,2,len(t.d)-2),',')" );
					sqlAppender.appendSql( "within group (order by t.k)+']' from (values" );
					char separator = ' ';
					for ( int i = 0; i < argumentsCount; i++ ) {
						sqlAppender.appendSql( separator );
						sqlAppender.appendSql( '(' );
						sqlAppender.appendSql( i );
						sqlAppender.appendSql( ",json_modify('[]','append $'," );
						renderValue( sqlAppender, sqlAstArguments.get( i ), walker );
						sqlAppender.appendSql( "))" );
						separator = ',';
					}
					sqlAppender.appendSql( ") t(k,d))" );
				}
				else {
					for ( int i = 0; i < argumentsCount; i++ ) {
						sqlAppender.appendSql( "json_modify(" );
					}
					sqlAppender.appendSql( "'[]'" );
					for ( int i = 0; i < argumentsCount; i++ ) {
						sqlAppender.appendSql( ",'append $'," );
						renderValue( sqlAppender, sqlAstArguments.get( i ), walker );
						sqlAppender.appendSql( ')' );
					}
				}
			}
		}
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		final JdbcMapping jdbcMapping = ((Expression) value).getExpressionType().getSingleJdbcMapping();
		final boolean isBoolean = ExpressionTypeHelper.isBoolean( value );
		if ( isBoolean ) {
			sqlAppender.appendSql( "cast(" );
		}
		((SQLServerAggregateSupport) walker.getSessionFactory().getJdbcServices().getDialect().getAggregateSupport())
				.appendJsonWriteExpression( sqlAppender, () -> value.accept( walker ), jdbcMapping );
		if ( isBoolean ) {
			sqlAppender.appendSql( " as bit)" );
		}
	}
}
