/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MariaDB json_value function.
 */
public class MariaDBJsonValueFunction extends JsonValueFunction {

	public MariaDBJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.NULL ) {
			// MariaDB reports the error 4038 as warning and simply returns null
			throw new QueryException( "Can't emulate on error clause on MariaDB" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on MariaDB" );
		}
		if ( arguments.returningType() != null ) {
			if ( arguments.returningType().getJdbcMapping().getJdbcType().isBoolean() ) {
				sqlAppender.append( "case " );
			}
			else {
				sqlAppender.append( "cast(" );
			}
		}
		sqlAppender.appendSql( "json_unquote(nullif(json_extract(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( "," );
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( passingClause == null ) {
			arguments.jsonPath().accept( walker );
		}
		else {
			JsonPathHelper.appendJsonPathConcatPassingClause(
					sqlAppender,
					arguments.jsonPath(),
					passingClause, walker
			);
		}
		sqlAppender.appendSql( "),'null'))" );
		if ( arguments.returningType() != null ) {
			if ( arguments.returningType().getJdbcMapping().getJdbcType().isBoolean() ) {
				sqlAppender.append( " when 'true' then true when 'false' then false end " );
			}
			else {
				sqlAppender.appendSql( " as " );
				arguments.returningType().accept( walker );
				sqlAppender.appendSql( ')' );
			}
		}
	}
}
