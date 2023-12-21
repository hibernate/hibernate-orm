/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryNodeProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;

public abstract class AbstractSqlAstQueryNodeProcessingStateImpl
		extends SqlAstProcessingStateImpl
		implements SqlAstQueryNodeProcessingState {

	private final Map<SqmFrom<?, ?>, Boolean> sqmFromRegistrations = new HashMap<>();

	public AbstractSqlAstQueryNodeProcessingStateImpl(
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, currentClauseAccess );
	}

	public AbstractSqlAstQueryNodeProcessingStateImpl(
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Function<SqlExpressionResolver, SqlExpressionResolver> expressionResolverDecorator,
			Supplier<Clause> currentClauseAccess) {
		super( parent, creationState, expressionResolverDecorator, currentClauseAccess );
	}

	@Override
	public void registerTreatedFrom(SqmFrom<?, ?> sqmFrom) {
		sqmFromRegistrations.put( sqmFrom, null );
	}

	@Override
	public void registerFromUsage(SqmFrom<?, ?> sqmFrom, boolean downgradeTreatUses) {
		if ( !( sqmFrom instanceof SqmTreatedPath<?, ?> ) ) {
			if ( !sqmFromRegistrations.containsKey( sqmFrom ) ) {
				final SqlAstProcessingState parentState = getParentState();
				if ( parentState instanceof SqlAstQueryPartProcessingState ) {
					( (SqlAstQueryPartProcessingState) parentState ).registerFromUsage( sqmFrom, downgradeTreatUses );
				}
			}
			else {
				// If downgrading was once forcibly disabled, don't overwrite that anymore
				final Boolean currentValue = sqmFromRegistrations.get( sqmFrom );
				if ( currentValue != Boolean.FALSE ) {
					sqmFromRegistrations.put( sqmFrom, downgradeTreatUses );
				}
			}
		}
	}

	@Override
	public Map<SqmFrom<?, ?>, Boolean> getFromRegistrations() {
		return sqmFromRegistrations;
	}

}
