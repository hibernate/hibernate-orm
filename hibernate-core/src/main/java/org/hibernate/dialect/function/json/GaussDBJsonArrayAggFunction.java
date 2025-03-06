/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_arrayagg function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonArrayAggFunction.
 */
public class GaussDBJsonArrayAggFunction extends JsonArrayAggFunction {

	private final boolean supportsStandard;

	public GaussDBJsonArrayAggFunction(boolean supportsStandard, TypeConfiguration typeConfiguration) {
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

		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );

		sqlAppender.appendSql( "to_jsonb( array_agg( CASE WHEN " );
		arrayExpression.accept( translator );
		sqlAppender.appendSql( " IS NOT NULL THEN " );
		arrayExpression.accept( translator );
		sqlAppender.appendSql( "::text ELSE NULL END" );
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
		sqlAppender.appendSql( ") ) AS result" );
	}
}
