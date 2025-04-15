/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL json_arrayagg function.
 */
public class PostgreSQLJsonArrayAggFunction extends JsonArrayAggFunction {

	private final boolean supportsStandard;

	public PostgreSQLJsonArrayAggFunction(boolean supportsStandard, TypeConfiguration typeConfiguration) {
		super( true, typeConfiguration );
		this.supportsStandard = supportsStandard;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		if ( supportsStandard ) {
			super.render( sqlAppender, sqlAstArguments, filter, withinGroup, returnType, translator );
		}
		else {
			final String jsonTypeName = translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry()
					.getTypeName( SqlTypes.JSON, translator.getSessionFactory().getJdbcServices().getDialect() );
			sqlAppender.appendSql( jsonTypeName );
			sqlAppender.appendSql( "_agg" );
			final JsonNullBehavior nullBehavior;
			if ( sqlAstArguments.size() > 1 ) {
				nullBehavior = (JsonNullBehavior) sqlAstArguments.get( 1 );
			}
			else {
				nullBehavior = JsonNullBehavior.ABSENT;
			}
			sqlAppender.appendSql( '(' );
			final SqlAstNode firstArg = sqlAstArguments.get( 0 );
			final Expression arg;
			if ( firstArg instanceof Distinct distinct ) {
				sqlAppender.appendSql( "distinct " );
				arg = distinct.getExpression();
			}
			else {
				arg = (Expression) firstArg;
			}
			renderArgument( sqlAppender, arg, nullBehavior, translator );
			if ( withinGroup != null && !withinGroup.isEmpty() ) {
				translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
				sqlAppender.appendSql( " order by " );
				withinGroup.get( 0 ).accept( translator );
				for ( int i = 1; i < withinGroup.size(); i++ ) {
					sqlAppender.appendSql( ',' );
					withinGroup.get( i ).accept( translator );
				}
				translator.getCurrentClauseStack().pop();
			}
			sqlAppender.appendSql( ')' );

			if ( filter != null ) {
				translator.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( translator );
				if ( nullBehavior != JsonNullBehavior.NULL ) {
					sqlAppender.appendSql( " and " );
					arg.accept( translator );
					sqlAppender.appendSql( " is not null" );
				}
				sqlAppender.appendSql( ')' );
				translator.getCurrentClauseStack().pop();
			}
			else if ( nullBehavior != JsonNullBehavior.NULL ) {
				sqlAppender.appendSql( " filter (where " );
				arg.accept( translator );
				sqlAppender.appendSql( " is not null)" );
			}
		}
	}
}
