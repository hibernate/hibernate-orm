/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
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
 * Standard json_object function.
 */
public class JsonObjectFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	protected final boolean colonSyntax;

	public JsonObjectFunction(TypeConfiguration typeConfiguration, boolean colonSyntax) {
		super(
				"json_object",
				FunctionKind.NORMAL,
				new JsonObjectArgumentsValidator(),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				),
				null
		);
		this.colonSyntax = colonSyntax;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_object" );
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
			for ( int i = 0; i < argumentsCount; i += 2 ) {
				sqlAppender.appendSql( separator );
				final SqlAstNode key = sqlAstArguments.get( i );
				final SqlAstNode value = sqlAstArguments.get( i + 1 );
				key.accept( walker );
				if ( colonSyntax ) {
					sqlAppender.appendSql( ':' );
				}
				else {
					sqlAppender.appendSql( " value " );
				}
				renderValue( sqlAppender, value, walker );
				separator = ',';
			}
			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				sqlAppender.appendSql( " absent on null" );
			}
		}
		sqlAppender.appendSql( ')' );
	}

	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		value.accept( walker );
	}
}
