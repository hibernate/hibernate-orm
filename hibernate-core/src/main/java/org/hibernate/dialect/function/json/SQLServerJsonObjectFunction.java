/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_object function.
 */
public class SQLServerJsonObjectFunction extends JsonObjectFunction {

	private final boolean supportsExtendedJson;

	public SQLServerJsonObjectFunction(boolean supportsExtendedJson, TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
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
				sqlAppender.appendSql( "'{}'" );
			}
			else {
				final JsonNullBehavior nullBehavior;
				final int argumentsCount;
				if ( ( sqlAstArguments.size() & 1 ) == 1 ) {
					nullBehavior = (JsonNullBehavior) sqlAstArguments.get( sqlAstArguments.size() - 1 );
					argumentsCount = sqlAstArguments.size() - 1;
				}
				else {
					nullBehavior = JsonNullBehavior.NULL;
					argumentsCount = sqlAstArguments.size();
				}
				if ( nullBehavior == JsonNullBehavior.ABSENT ) {
					for ( int i = 0; i < argumentsCount; i += 2 ) {
						sqlAppender.appendSql( "json_modify(" );
					}
					sqlAppender.appendSql( "'{}'" );
					for ( int i = 0; i < argumentsCount; i += 2 ) {
						sqlAppender.appendSql( ",'$.'+" );
						sqlAstArguments.get( i ).accept( walker );
						sqlAppender.appendSql( ',' );
						renderValue( sqlAppender, sqlAstArguments.get( i + 1 ), walker );
						sqlAppender.appendSql( ')' );
					}
				}
				else {
					sqlAppender.appendSql( "(select '{'+string_agg(substring(t.k,2,len(t.k)-2)" );
					sqlAppender.appendSql( "+':'+substring(t.d,2,len(t.d)-2),',')+'}' from (values" );
					char separator = ' ';
					for ( int i = 0; i < argumentsCount; i += 2 ) {
						sqlAppender.appendSql( separator );
						sqlAppender.appendSql( "(json_modify('[]','append $'," );
						sqlAstArguments.get( i ).accept( walker );
						sqlAppender.appendSql( "),json_modify('[]','append $'," );
						renderValue( sqlAppender, sqlAstArguments.get( i + 1 ), walker );
						sqlAppender.appendSql( "))" );
						separator = ',';
					}
					sqlAppender.appendSql( ") t(k,d))" );
				}
			}
		}
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( ExpressionTypeHelper.isBoolean( value ) ) {
			sqlAppender.appendSql( "cast(" );
			value.accept( walker );
			sqlAppender.appendSql( " as bit)" );
		}
		else if ( !supportsExtendedJson && ExpressionTypeHelper.isJson( value ) ) {
			sqlAppender.appendSql( "json_query(" );
			value.accept( walker );
			sqlAppender.appendSql( ')' );
		}
		else {
			value.accept( walker );
		}
	}
}
