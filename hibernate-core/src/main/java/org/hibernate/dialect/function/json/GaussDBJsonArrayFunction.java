/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;


/**
 * Notes: Original code of this class is based on JsonArrayFunction.
 */
public class GaussDBJsonArrayFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public GaussDBJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super(
				"json_array",
				FunctionKind.NORMAL,
				null,
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				),
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_array" );
		char separator = '(';
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( separator );
			renderReturningClause( sqlAppender, walker );
		}
		else {
			final SqlAstNode lastArgument = sqlAstArguments.get( sqlAstArguments.size() - 1 );
			final JsonNullBehavior nullBehavior;
			final int argumentsCount;
			if ( lastArgument instanceof JsonNullBehavior ) {
				nullBehavior = (JsonNullBehavior) lastArgument;
				argumentsCount = sqlAstArguments.size() - 1;
			}
			else {
				nullBehavior = JsonNullBehavior.ABSENT;
				argumentsCount = sqlAstArguments.size();
			}
			for ( int i = 0; i < argumentsCount; i++ ) {
				Expression valueNode = (Expression) sqlAstArguments.get( i );
				if ( nullBehavior ==  JsonNullBehavior.ABSENT && valueNode instanceof Literal ) {
					Object literalValue = ((Literal) valueNode).getLiteralValue();
					if ( literalValue == null ) {
						continue;
					}
				}
				sqlAppender.appendSql( separator );
				valueNode.accept( walker );
				separator = ',';
			}
			renderReturningClause( sqlAppender, walker );
		}
		sqlAppender.appendSql( ')' );
	}

	protected void renderReturningClause(SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		// No-op
	}
}
