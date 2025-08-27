/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

import java.util.List;
import java.util.Locale;

import static java.util.Collections.emptyList;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class NamedSqmFunctionDescriptor
		extends AbstractSqmSelfRenderingFunctionDescriptor {
	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;
	private final String argumentListSignature;
	private final SqlAstNodeRenderingMode argumentRenderingMode;

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nullable FunctionReturnTypeResolver returnTypeResolver) {
		this(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				returnTypeResolver,
				null,
				functionName,
				FunctionKind.NORMAL,
				null,
				SqlAstNodeRenderingMode.DEFAULT
		);
	}

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nullable FunctionReturnTypeResolver returnTypeResolver,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver) {
		this(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				returnTypeResolver,
				argumentTypeResolver,
				functionName,
				FunctionKind.NORMAL,
				null,
				SqlAstNodeRenderingMode.DEFAULT
		);
	}

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			@Nullable ArgumentsValidator argumentsValidator,
			@Nullable FunctionReturnTypeResolver returnTypeResolver,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver,
			String name,
			FunctionKind functionKind,
			String argumentListSignature,
			SqlAstNodeRenderingMode argumentRenderingMode) {
		super( name, functionKind, argumentsValidator, returnTypeResolver, argumentTypeResolver );

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		this.argumentListSignature = argumentListSignature;
		this.argumentRenderingMode = argumentRenderingMode;
	}

	/**
	 * Function name accessor
	 *
	 * @return The function name.
	 */
	public String getName() {
		return functionName;
	}

	@Override
	public String getArgumentListSignature() {
		return argumentListSignature == null
				? super.getArgumentListSignature()
				: argumentListSignature;
	}

	@Override
	public boolean alwaysIncludesParentheses() {
		return useParenthesesWhenNoArgs;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, sqlAstArguments, null, emptyList(), null, null, translator );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, sqlAstArguments, filter, emptyList(), null, null, translator );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		render( sqlAppender, sqlAstArguments, filter, withinGroup, null, null, translator );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, filter, emptyList(), respectNulls, fromFirst, walker );
	}

	private void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			Boolean respectNulls,
			Boolean fromFirst,
			SqlAstTranslator<?> translator) {
		final boolean useParens = useParenthesesWhenNoArgs || !sqlAstArguments.isEmpty();
		final boolean caseWrapper =
				filter != null
					&& !translator.getSessionFactory().getJdbcServices().getDialect()
							.supportsFilterClause();

		sqlAppender.appendSql( functionName );
		if ( useParens ) {
			sqlAppender.appendSql( "(" );
		}

		boolean firstPass = true;
		for ( SqlAstNode arg : sqlAstArguments ) {
			if ( !firstPass ) {
				sqlAppender.appendSql( "," );
			}
			if ( caseWrapper && !( arg instanceof Distinct ) ) {
				translator.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( "case when " );
				filter.accept( translator );
				translator.getCurrentClauseStack().pop();
				sqlAppender.appendSql( " then " );
				if ( ( arg instanceof Star ) ) {
					sqlAppender.appendSql( "1" );
				}
				else {
					translator.render( arg, argumentRenderingMode );
				}
				sqlAppender.appendSql( " else null end" );
			}
			else {
				translator.render( arg, argumentRenderingMode );
			}
			firstPass = false;
		}

		if ( useParens ) {
			sqlAppender.appendSql( ")" );
		}

		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " within group (order by " );
			translator.render( withinGroup.get( 0 ), argumentRenderingMode );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( SqlAppender.COMMA_SEPARATOR_CHAR );
				translator.render( withinGroup.get( 0 ), argumentRenderingMode );
			}
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}

		if ( fromFirst != null ) {
			if ( fromFirst ) {
				sqlAppender.appendSql( " from first" );
			}
			else {
				sqlAppender.appendSql( " from last" );
			}
		}
		if ( respectNulls != null ) {
			if ( respectNulls ) {
				sqlAppender.appendSql( " respect nulls" );
			}
			else {
				sqlAppender.appendSql( " ignore nulls" );
			}
		}

		if ( filter != null && !caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"NamedSqmFunctionDescriptor(%s)",
				functionName
		);
	}

}
