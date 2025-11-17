/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter.SqmAliasedNodeCollector;
import org.hibernate.query.sqm.sql.ConversionException;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Implementation of ProcessingState used on its own as the impl for
 * DML statements and as the base for QuerySpec state
 *
 * @author Steve Ebersole
 */
public class SqlAstProcessingStateImpl
		implements SqlAstProcessingState, SqlExpressionResolver, SqmAliasedNodeCollector {
	private final SqlAstProcessingState parentState;
	private final SqlAstCreationState creationState;
	private final SqlExpressionResolver expressionResolver;
	private final Supplier<Clause> currentClauseAccess;

	private final Map<ColumnReferenceKey, Expression> expressionMap = new HashMap<>();

	public SqlAstProcessingStateImpl(
			SqlAstProcessingState parentState,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess) {
		this.parentState = parentState;
		this.creationState = creationState;
		this.expressionResolver = this;
		this.currentClauseAccess = currentClauseAccess;
	}

	public SqlAstProcessingStateImpl(
			SqlAstProcessingState parentState,
			SqlAstCreationState creationState,
			Function<SqlExpressionResolver, SqlExpressionResolver> expressionResolverDecorator,
			Supplier<Clause> currentClauseAccess) {
		this.parentState = parentState;
		this.creationState = creationState;
		this.expressionResolver = expressionResolverDecorator.apply( this );
		this.currentClauseAccess = currentClauseAccess;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ProcessingState

	@Override
	public SqlAstProcessingState getParentState() {
		return parentState;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return expressionResolver;
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return creationState;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	@Override
	public Expression resolveSqlExpression(
			ColumnReferenceKey key,
			Function<SqlAstProcessingState,Expression> creator) {
		final Expression existing = expressionMap.get( key );

		final Expression expression;
		if ( existing != null ) {
			expression = existing;
		}
		else {
			expression = creator.apply( this );
			expressionMap.put( key, expression );
		}

		return expression;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			FetchParent fetchParent, TypeConfiguration typeConfiguration) {
		throw new ConversionException( "Unexpected call to resolve SqlSelection outside of QuerySpec processing" );
	}

	@Override
	public void next() {
		// nothing to do
		int i = 1;
	}

	@Override
	public List<SqlSelection> getSelections(int position) {
		throw new UnsupportedOperationException();
	}
}
