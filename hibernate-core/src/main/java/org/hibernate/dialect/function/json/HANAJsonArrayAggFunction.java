/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SAP HANA json_arrayagg function.
 */
public class HANAJsonArrayAggFunction extends JsonArrayAggFunction {

	public HANAJsonArrayAggFunction(TypeConfiguration typeConfiguration) {
		super( false, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null;
		sqlAppender.appendSql( "'['||string_agg(" );
		final JsonNullBehavior nullBehavior;
		if ( sqlAstArguments.size() > 1 ) {
			nullBehavior = (JsonNullBehavior) sqlAstArguments.get( 1 );
		}
		else {
			nullBehavior = JsonNullBehavior.ABSENT;
		}
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = distinct.getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		if ( caseWrapper ) {
			if ( nullBehavior != JsonNullBehavior.ABSENT ) {
				throw new QueryException( "Can't emulate json_arrayagg filter clause when using 'null on null' clause." );
			}
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			renderArgument( sqlAppender, arg, nullBehavior, translator );
			sqlAppender.appendSql( " else null end)" );
		}
		else {
			renderArgument( sqlAppender, arg, nullBehavior, translator );
		}
		sqlAppender.appendSql( ",','" );
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
		sqlAppender.appendSql( ")||']'" );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		// Convert the value to JSON
		final JdbcMappingContainer expressionType = arg.getExpressionType();
		if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson() ) {
			sqlAppender.appendSql( "cast(" );
			arg.accept( translator );
			sqlAppender.appendSql( " as nvarchar(" + Integer.MAX_VALUE + "))" );
		}
		else {
			sqlAppender.appendSql( "json_query((select " );
			arg.accept( translator );
			sqlAppender.appendSql(
					" V from sys.dummy for json('arraywrap'='no','omitnull'='no') returns nvarchar(" + Integer.MAX_VALUE + ")),'$.V')" );
		}
	}
}
