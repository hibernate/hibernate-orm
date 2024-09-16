/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * SQL Server json_replace function.
 */
public class SQLServerJsonReplaceFunction extends AbstractJsonReplaceFunction {

	public SQLServerJsonReplaceFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		sqlAppender.appendSql( "(select case when coalesce(json_query(t.d,t.p),json_value(t.d,t.p)) is null then t.d else json_modify(t.d,t.p," );
		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		final SqlAstNode value = arguments.get( 2 );
		renderValue( sqlAppender, value, translator );
		sqlAppender.appendSql( ") end from (values(");
		json.accept( translator );
		sqlAppender.appendSql( ',' );
		jsonPath.accept( translator );
		sqlAppender.appendSql( ")) t(d,p))" );
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
