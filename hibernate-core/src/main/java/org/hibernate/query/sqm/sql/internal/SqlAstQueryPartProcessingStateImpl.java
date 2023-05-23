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
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqlAstQueryPartProcessingStateImpl
		extends SqlAstProcessingStateImpl
		implements SqlAstQueryPartProcessingState {

	private final QueryPart queryPart;
	private final Map<SqmFrom<?, ?>, Boolean> sqmFromRegistrations = new HashMap<>();
	private final boolean deduplicateSelectionItems;
	private FetchParent nestingFetchParent;

	public SqlAstQueryPartProcessingStateImpl(
			QueryPart queryPart,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Supplier<Clause> currentClauseAccess,
			boolean deduplicateSelectionItems) {
		super( parent, creationState, currentClauseAccess );
		this.queryPart = queryPart;
		this.deduplicateSelectionItems = deduplicateSelectionItems;
	}

	public SqlAstQueryPartProcessingStateImpl(
			QueryPart queryPart,
			SqlAstProcessingState parent,
			SqlAstCreationState creationState,
			Function<SqlExpressionResolver, SqlExpressionResolver> expressionResolverDecorator,
			Supplier<Clause> currentClauseAccess,
			boolean deduplicateSelectionItems) {
		super( parent, creationState, expressionResolverDecorator, currentClauseAccess );
		this.queryPart = queryPart;
		this.deduplicateSelectionItems = deduplicateSelectionItems;
	}

	public FetchParent getNestingFetchParent() {
		return nestingFetchParent;
	}

	public void setNestingFetchParent(FetchParent nestedParent) {
		this.nestingFetchParent = nestedParent;
	}

	@Override
	public QueryPart getInflightQueryPart() {
		return queryPart;
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	private Map<?, ?> sqlSelectionMap;

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			FetchParent fetchParent,
			TypeConfiguration typeConfiguration) {
		if ( nestingFetchParent != null ) {
			final String selectableName;
			if ( expression instanceof ColumnReference ) {
				selectableName = ( (ColumnReference) expression ).getSelectableName();
			}
			else {
				throw new IllegalArgumentException( "Illegal expression passed for nested fetching: " + expression );
			}
			return expression.createSqlSelection(
					-1,
					nestingFetchParent.getReferencedMappingType().getSelectableIndex( selectableName ),
					javaType,
					typeConfiguration
			);
		}
		final Map<Expression, SqlSelection> selectionMap;
		if ( deduplicateSelectionItems ) {
			final SqlSelection existing;
			if ( sqlSelectionMap == null ) {
				sqlSelectionMap = new HashMap<>();
				existing = null;
			}
			else {
				existing = (SqlSelection) sqlSelectionMap.get( expression );
			}

			if ( existing != null ) {
				return existing;
			}
			//noinspection unchecked
			selectionMap = (Map<Expression, SqlSelection>) sqlSelectionMap;
		}
		else if ( fetchParent != null ) {
			// De-duplicate selection items within the root of a fetch parent
			final Map<FetchParent, Map<Expression, SqlSelection>> fetchParentSqlSelectionMap;
			final FetchParent root = fetchParent.getRoot();
			if ( sqlSelectionMap == null ) {
				sqlSelectionMap = fetchParentSqlSelectionMap = new HashMap<>();
				fetchParentSqlSelectionMap.put( root, selectionMap = new HashMap<>() );
			}
			else {
				//noinspection unchecked
				fetchParentSqlSelectionMap = (Map<FetchParent, Map<Expression, SqlSelection>>) sqlSelectionMap;
				final Map<Expression, SqlSelection> map = fetchParentSqlSelectionMap.get( root );
				if ( map == null ) {
					fetchParentSqlSelectionMap.put( root, selectionMap = new HashMap<>() );
				}
				else {
					selectionMap = map;
				}
			}
			final SqlSelection sqlSelection = selectionMap.get( expression );
			if ( sqlSelection != null ) {
				return sqlSelection;
			}
		}
		else {
			selectionMap = null;
		}

		final SelectClause selectClause = ( (QuerySpec) queryPart ).getSelectClause();
		final int valuesArrayPosition = selectClause.getSqlSelections().size();
		final SqlSelection sqlSelection;
		if ( isTopLevel() ) {
			sqlSelection = expression.createDomainResultSqlSelection(
					valuesArrayPosition + 1,
					valuesArrayPosition,
					javaType,
					typeConfiguration
			);
		}
		else {
			sqlSelection = expression.createSqlSelection(
					valuesArrayPosition + 1,
					valuesArrayPosition,
					javaType,
					typeConfiguration
			);
		}

		selectClause.addSqlSelection( sqlSelection );

		if ( selectionMap != null ) {
			selectionMap.put( expression, sqlSelection );
		}

		return sqlSelection;
	}
}
