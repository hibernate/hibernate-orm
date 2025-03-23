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
 * DB2 json_objectagg function.
 */
public class DB2JsonObjectAggFunction extends JsonObjectAggFunction {

	public DB2JsonObjectAggFunction(TypeConfiguration typeConfiguration) {
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
		sqlAppender.appendSql( "'{'||listagg(" );
		renderArgument( sqlAppender, arguments.key(), arguments.nullBehavior(), translator );
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
		if ( nullBehavior == JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( "coalesce(" );
		}
		final JdbcMappingContainer expressionType = arg.getExpressionType();
		if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson() ) {
			arg.accept( translator );
		}
		else if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isBinary() ) {
			sqlAppender.appendSql( "json_query(json_array(rawtohex(" );
			arg.accept( translator );
			sqlAppender.appendSql( ") null on null),'$.*')" );
		}
		else {
			sqlAppender.appendSql( "json_query(json_array(" );
			arg.accept( translator );
			sqlAppender.appendSql( " null on null),'$.*')" );
		}
		if ( nullBehavior == JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( ",'null')" );
		}
	}
}
