/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard json_array function.
 */
public class JsonArrayFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public JsonArrayFunction(TypeConfiguration typeConfiguration) {
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
				sqlAppender.appendSql( separator );
				renderValue( sqlAppender, sqlAstArguments.get( i ), walker );
				separator = ',';
			}
			if ( nullBehavior == JsonNullBehavior.NULL ) {
				sqlAppender.appendSql( " null on null" );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		value.accept( walker );
	}
}
