/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.JsonObjectAggFunction;
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
 * SingleStore json_objectagg function.
 */
public class SingleStoreJsonObjectAggFunction extends JsonObjectAggFunction {

	public SingleStoreJsonObjectAggFunction(TypeConfiguration typeConfiguration) {
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
		sqlAppender.appendSql( "concat('{',group_concat(concat(to_json(" );
		arguments.key().accept( translator );
		sqlAppender.appendSql( "),':'," );
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
		sqlAppender.appendSql( ") separator ','),'}')" );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender, Expression arg, JsonNullBehavior nullBehavior, SqlAstTranslator<?> translator) {
		sqlAppender.appendSql( "to_json(" );
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( "nullif(" );
		}
		arg.accept( translator );
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( ",'null')" );
		}
		sqlAppender.appendSql( ")" );
	}
}
