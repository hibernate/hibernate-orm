/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_array_append function.
 */
public class SQLServerJsonArrayAppendFunction extends AbstractJsonArrayAppendFunction {

	public SQLServerJsonArrayAppendFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
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
		sqlAppender.appendSql("json_modify(t.d,t.p,json_query('['+coalesce(json_value(t.d,t.p),case when json_path_exists(t.d,t.p)=1 then 'null' end)+stuff(json_array(t.v),1,1,','))),");
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
		else {
			value.accept( translator );
		}
	}
}
