/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_array_append function.
 */
public class SQLServerJsonArrayAppendFunction extends AbstractJsonArrayAppendFunction {

	private final boolean supportsExtendedJson;

	public SQLServerJsonArrayAppendFunction(boolean supportsExtendedJson, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.supportsExtendedJson = supportsExtendedJson;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "(select coalesce(" );
		sqlAppender.appendSql("case when json_modify(json_query(t.d,t.p),'append $',t.v) is not null then json_modify(t.d,t.p,json_modify(json_query(t.d,t.p),'append $',t.v)) end,");
		sqlAppender.appendSql("json_modify(t.d,t.p,json_query('['+coalesce(json_value(t.d,t.p),case when ");
		if ( supportsExtendedJson ) {
			sqlAppender.appendSql( "json_path_exists(t.d,t.p)=1" );
		}
		else {
			final List<JsonPathHelper.JsonPathElement> pathElements =
					JsonPathHelper.parseJsonPathElements( translator.getLiteralValue( jsonPath ) );
			final JsonPathHelper.JsonPathElement lastPathElement = pathElements.get( pathElements.size() - 1 );
			final String prefix = JsonPathHelper.toJsonPath( pathElements, 0, pathElements.size() - 1 );
			final String terminalKey;
			if ( lastPathElement instanceof JsonPathHelper.JsonIndexAccess indexAccess ) {
				terminalKey = String.valueOf( indexAccess.index() );
			}
			else if (lastPathElement instanceof JsonPathHelper.JsonAttribute attribute) {
				terminalKey = attribute.attribute();
			}
			else {
				throw new AssertionFailure( "Unrecognized json path element: " + lastPathElement );
			}

			sqlAppender.appendSql( "(select 1 from openjson(t.d," );
			sqlAppender.appendSingleQuoteEscapedString( prefix );
			sqlAppender.appendSql( ") t where t.[key]=" );
			sqlAppender.appendSingleQuoteEscapedString( terminalKey );
			sqlAppender.appendSql( ")=1" );
		}
		sqlAppender.appendSql( " then 'null' end)+stuff(json_modify('[]','append $',t.v),1,1,','))),");
		sqlAppender.appendSql( "t.d) from (values (" );
		json.accept( translator );
		sqlAppender.appendSql( ',' );
		jsonPath.accept( translator );
		sqlAppender.appendSql( ',' );
		renderValue( sqlAppender, value, translator );
		sqlAppender.appendSql( ")) t(d,p,v))" );
	}

	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> translator) {
		if ( ExpressionTypeHelper.isBoolean( value ) ) {
			sqlAppender.appendSql( "cast(" );
			value.accept( translator );
			sqlAppender.appendSql( " as bit)" );
		}
		else if ( ExpressionTypeHelper.isJson( value ) ) {
			sqlAppender.appendSql( "json_query(" );
			value.accept( translator );
			sqlAppender.appendSql( ')' );
		}
		else {
			value.accept( translator );
		}
	}
}
