/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_value function.
 */
public class SQLServerJsonValueFunction extends JsonValueFunction {

	public SQLServerJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// openjson errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on SQL server" );
		}
		sqlAppender.appendSql( "(select v from openjson(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ",'$') with (v " );
		if ( arguments.returningType() != null ) {
			arguments.returningType().accept( walker );
		}
		else {
			sqlAppender.appendSql( "varchar(max)" );
		}
		sqlAppender.appendSql( ' ' );
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
					sqlAppender,
					"strict " + walker.getLiteralValue( arguments.jsonPath() )
			);
		}
		else {
			arguments.jsonPath().accept( walker );
		}
		sqlAppender.appendSql( "))" );
	}
}
