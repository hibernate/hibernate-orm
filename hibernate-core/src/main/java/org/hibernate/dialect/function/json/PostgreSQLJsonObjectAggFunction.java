/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.sql.ast.tree.expression.JsonObjectAggUniqueKeysBehavior;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL json_objectagg function.
 */
public class PostgreSQLJsonObjectAggFunction extends JsonObjectAggFunction {

	private final boolean supportsStandard;

	public PostgreSQLJsonObjectAggFunction(boolean supportsStandard, TypeConfiguration typeConfiguration) {
		super( ":", true, typeConfiguration );
		this.supportsStandard = supportsStandard;
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		if ( supportsStandard ) {
			super.render( sqlAppender, arguments, filter, returnType, translator );
		}
		else {
			if ( arguments.uniqueKeysBehavior() == JsonObjectAggUniqueKeysBehavior.WITH ) {
				throw new QueryException( "Can't emulate json_objectagg 'with unique keys' clause." );
			}
			final String jsonTypeName = translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry()
					.getTypeName( SqlTypes.JSON, translator.getSessionFactory().getJdbcServices().getDialect() );
			sqlAppender.appendSql( jsonTypeName );
			sqlAppender.appendSql( "_object_agg" );
			sqlAppender.appendSql( '(' );
			arguments.key().accept( translator );
			sqlAppender.appendSql( ',' );
			arguments.value().accept( translator );
			sqlAppender.appendSql( ')' );

			if ( filter != null ) {
				translator.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( translator );
				if ( arguments.nullBehavior() != JsonNullBehavior.NULL ) {
					sqlAppender.appendSql( " and " );
					arguments.value().accept( translator );
					sqlAppender.appendSql( " is not null" );
				}
				sqlAppender.appendSql( ')' );
				translator.getCurrentClauseStack().pop();
			}
			else if ( arguments.nullBehavior() != JsonNullBehavior.NULL ) {
				sqlAppender.appendSql( " filter (where " );
				arguments.value().accept( translator );
				sqlAppender.appendSql( " is not null)" );
			}
		}
	}

	@Override
	protected void renderUniqueAndReturningClause(SqlAppender sqlAppender, JsonObjectAggArguments arguments, SqlAstTranslator<?> translator) {
		renderUniqueClause( sqlAppender, arguments, translator );
		renderReturningClause( sqlAppender, arguments, translator );
	}
}
