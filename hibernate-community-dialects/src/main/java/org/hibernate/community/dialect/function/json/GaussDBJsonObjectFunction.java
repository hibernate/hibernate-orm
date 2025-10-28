/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import org.hibernate.dialect.function.json.JsonObjectFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * GaussDB json_object function.
 * @author chenzhida
 *
 * Notes: Original code of this class is based on PostgreSQLJsonObjectFunction.
 */
public class GaussDBJsonObjectFunction extends JsonObjectFunction {

	public GaussDBJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		sqlAppender.appendSql( "json_build_object" );
		char separator = '(';
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( separator );
		}
		else {
			final JsonNullBehavior nullBehavior;
			final int argumentsCount;
			if ( ( sqlAstArguments.size() & 1 ) == 1 ) {
				nullBehavior = (JsonNullBehavior) sqlAstArguments.get( sqlAstArguments.size() - 1 );
				argumentsCount = sqlAstArguments.size() - 1;
			}
			else {
				nullBehavior = JsonNullBehavior.NULL;
				argumentsCount = sqlAstArguments.size();
			}
			sqlAppender.appendSql('(');
			separator = ' ';
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				final SqlAstNode key = sqlAstArguments.get( i );
				Expression valueNode = (Expression) sqlAstArguments.get( i+1 );
				if ( nullBehavior == JsonNullBehavior.ABSENT && walker.getLiteralValue( valueNode ) == null) {
					continue;
				}
				if (separator != ' ') {
					sqlAppender.appendSql(separator);
				}
				else {
					separator = ',';
				}
				key.accept( walker );
				sqlAppender.appendSql( ',' );
				valueNode.accept( walker );
			}
		}
		sqlAppender.appendSql( ')' );
	}

}
