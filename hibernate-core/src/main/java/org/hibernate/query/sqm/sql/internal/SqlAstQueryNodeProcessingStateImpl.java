/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

public class SqlAstQueryNodeProcessingStateImpl
		extends AbstractSqlAstQueryNodeProcessingStateImpl {

	private final FromClause fromClause;
	private Predicate predicate;

	public SqlAstQueryNodeProcessingStateImpl(
			FromClause fromClause,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, currentClauseAccess );
		this.fromClause = fromClause;
	}

	public SqlAstQueryNodeProcessingStateImpl(
			FromClause fromClause,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Function<SqlExpressionResolver, SqlExpressionResolver> expressionResolverDecorator,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, expressionResolverDecorator, currentClauseAccess );
		this.fromClause = fromClause;
	}

	@Override
	public FromClause getFromClause() {
		return fromClause;
	}

	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public void applyPredicate(Predicate predicate) {
		this.predicate = Predicate.combinePredicates( this.predicate, predicate );
	}
}
