/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.sql.ast.tree.expression.JsonObjectAggUniqueKeysBehavior;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Standard json_objectagg function.
 */
public class JsonObjectAggFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	protected final String valueSeparator;
	protected final boolean supportsFilter;

	public JsonObjectAggFunction(String valueSeparator, boolean supportsFilter, TypeConfiguration typeConfiguration) {
		super(
				"json_objectagg",
				FunctionKind.AGGREGATE,
				StandardArgumentsValidators.between( 2, 4 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				),
				null
		);
		this.supportsFilter = supportsFilter;
		this.valueSeparator = valueSeparator;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, JsonObjectAggArguments.extract( sqlAstArguments ), filter, returnType, translator );
	}

	protected void render(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.getSessionFactory().getJdbcServices().getDialect().supportsFilterClause();
		sqlAppender.appendSql( "json_objectagg(" );
		arguments.key().accept( translator );
		sqlAppender.appendSql( valueSeparator );
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
		if ( arguments.nullBehavior() == JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( " null on null" );
		}
		else {
			sqlAppender.appendSql( " absent on null" );
		}
		renderUniqueAndReturningClause( sqlAppender, arguments, translator );
		sqlAppender.appendSql( ')' );

		if ( !caseWrapper && filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		arg.accept( translator );
	}

	protected void renderUniqueAndReturningClause(SqlAppender sqlAppender, JsonObjectAggArguments arguments, SqlAstTranslator<?> translator) {
		renderReturningClause( sqlAppender, arguments, translator );
		renderUniqueClause( sqlAppender, arguments, translator );
	}

	protected void renderReturningClause(SqlAppender sqlAppender, JsonObjectAggArguments arguments, SqlAstTranslator<?> translator) {
		sqlAppender.appendSql( " returning " );
		sqlAppender.appendSql(
				translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry()
						.getTypeName( SqlTypes.JSON, translator.getSessionFactory().getJdbcServices().getDialect() )
		);
	}

	protected void renderUniqueClause(SqlAppender sqlAppender, JsonObjectAggArguments arguments, SqlAstTranslator<?> translator) {
		if ( arguments.uniqueKeysBehavior() == JsonObjectAggUniqueKeysBehavior.WITH ) {
			sqlAppender.appendSql( " with unique keys" );
		}
	}

	protected record JsonObjectAggArguments(
			Expression key,
			Expression value,
			@Nullable JsonNullBehavior nullBehavior,
			@Nullable JsonObjectAggUniqueKeysBehavior uniqueKeysBehavior) {
		public static JsonObjectAggArguments extract(List<? extends SqlAstNode> sqlAstArguments) {
			int nextIndex = 2;
			JsonNullBehavior nullBehavior = null;
			JsonObjectAggUniqueKeysBehavior uniqueKeysBehavior = null;
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonNullBehavior jsonNullBehavior ) {
					nullBehavior = jsonNullBehavior;
					nextIndex++;
				}
			}
			if ( nextIndex < sqlAstArguments.size() ) {
				final SqlAstNode node = sqlAstArguments.get( nextIndex );
				if ( node instanceof JsonObjectAggUniqueKeysBehavior jsonObjectAggUniqueKeysBehavior ) {
					uniqueKeysBehavior = jsonObjectAggUniqueKeysBehavior;
					nextIndex++;
				}
			}
			return new JsonObjectAggArguments(
					(Expression) sqlAstArguments.get( 0 ),
					(Expression) sqlAstArguments.get( 1 ),
					nullBehavior,
					uniqueKeysBehavior
			);
		}
	}
}
