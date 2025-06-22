/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.sql.ast.tree.expression.JsonObjectAggUniqueKeysBehavior;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SAP HANA json_objectagg function.
 */
public class HANAJsonObjectAggFunction extends JsonObjectAggFunction {

	public HANAJsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( ",", false, typeConfiguration );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null;
		if ( arguments.uniqueKeysBehavior() == JsonObjectAggUniqueKeysBehavior.WITH ) {
			throw new QueryException( "Can't emulate json_objectagg 'with unique keys' clause." );
		}
		sqlAppender.appendSql( "'{'||string_agg(" );
		renderArgument( sqlAppender, arguments.key(), JsonNullBehavior.NULL, translator );
		sqlAppender.appendSql( "||':'||" );
		if ( caseWrapper ) {
			if ( arguments.nullBehavior() != JsonNullBehavior.ABSENT ) {
				throw new QueryException( "Can't emulate json_objectagg filter clause when using 'null on null' clause." );
			}
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			renderArgument( sqlAppender, arguments.value(), arguments.nullBehavior(), translator );
			sqlAppender.appendSql( " else null end)" );
		}
		else {
			renderArgument( sqlAppender, arguments.value(), arguments.nullBehavior(), translator );
		}
		sqlAppender.appendSql( ",',')||'}'" );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		// Convert SQL type to JSON type
		final JdbcMappingContainer expressionType = arg.getExpressionType();
		if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson() ) {
			sqlAppender.appendSql( "cast(" );
			arg.accept( translator );
			sqlAppender.appendSql( " as nvarchar(" + Integer.MAX_VALUE + "))" );
		}
		else {
			sqlAppender.appendSql( "json_query((select " );
			arg.accept( translator );
			sqlAppender.appendSql( " V from sys.dummy for json('arraywrap'='no'" );
			if ( nullBehavior == JsonNullBehavior.NULL ) {
				sqlAppender.appendSql( ",'omitnull'='no'" );
			}
			sqlAppender.appendSql( ") returns nvarchar(" + Integer.MAX_VALUE + ")),'$.V')" );
		}
	}
}
