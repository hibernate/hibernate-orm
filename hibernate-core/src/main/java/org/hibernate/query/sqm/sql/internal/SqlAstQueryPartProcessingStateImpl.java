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

import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.predicate.Predicate;
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
		extends AbstractSqlAstQueryNodeProcessingStateImpl
		implements SqlAstQueryPartProcessingState {

	private final QueryPart queryPart;
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
	public FromClause getFromClause() {
		return queryPart.getLastQuerySpec().getFromClause();
	}

	@Override
	public void applyPredicate(Predicate predicate) {
		queryPart.getLastQuerySpec().applyPredicate( predicate );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	private Map<?, ?> sqlSelectionMap;
	private int nextJdbcPosition = 1;

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
			final int selectableIndex = nestingFetchParent.getReferencedMappingType().getSelectableIndex( selectableName );
			if ( selectableIndex != -1 ) {
				return expression.createSqlSelection(
						-1,
						selectableIndex,
						javaType,
						true,
						typeConfiguration
				);
			}
		}
		final Map<Expression, Object> selectionMap;
		if ( deduplicateSelectionItems ) {
			if ( sqlSelectionMap == null ) {
				sqlSelectionMap = new HashMap<>();
			}
			//noinspection unchecked
			selectionMap = (Map<Expression, Object>) sqlSelectionMap;
		}
		else if ( fetchParent != null ) {
			// De-duplicate selection items within the root of a fetch parent
			final Map<FetchParent, Map<Expression, Object>> fetchParentSqlSelectionMap;
			final FetchParent root = fetchParent.getRoot();
			if ( sqlSelectionMap == null ) {
				sqlSelectionMap = fetchParentSqlSelectionMap = new HashMap<>();
				fetchParentSqlSelectionMap.put( root, selectionMap = new HashMap<>() );
			}
			else {
				//noinspection unchecked
				fetchParentSqlSelectionMap = (Map<FetchParent, Map<Expression, Object>>) sqlSelectionMap;
				final Map<Expression, Object> map = fetchParentSqlSelectionMap.get( root );
				if ( map == null ) {
					fetchParentSqlSelectionMap.put( root, selectionMap = new HashMap<>() );
				}
				else {
					selectionMap = map;
				}
			}
		}
		else {
			selectionMap = null;
		}

		final int jdbcPosition;
		final Object existingSelection;
		if ( selectionMap != null ) {
			existingSelection = selectionMap.get( expression );
			if ( existingSelection != null ) {
				if ( existingSelection instanceof SqlSelection ) {
					final SqlSelection sqlSelection = (SqlSelection) existingSelection;
					if ( sqlSelection.getExpressionType() == expression.getExpressionType() ) {
						return sqlSelection;
					}
					jdbcPosition = sqlSelection.getJdbcResultSetIndex();
				}
				else {
					final SqlSelection[] selections = (SqlSelection[]) existingSelection;
					for ( SqlSelection sqlSelection : selections ) {
						if ( sqlSelection.getExpressionType() == expression.getExpressionType() ) {
							return sqlSelection;
						}
					}
					jdbcPosition = selections[0].getJdbcResultSetIndex();
				}
			}
			else {
				jdbcPosition = nextJdbcPosition++;
			}
		}
		else {
			jdbcPosition = nextJdbcPosition++;
			existingSelection = null;
		}
		final boolean virtual = existingSelection != null;
		final SelectClause selectClause = ( (QuerySpec) queryPart ).getSelectClause();
		final int valuesArrayPosition = selectClause.getSqlSelections().size();
		final SqlSelection sqlSelection;
		if ( isTopLevel() ) {
			sqlSelection = expression.createDomainResultSqlSelection(
					jdbcPosition,
					valuesArrayPosition,
					javaType,
					virtual,
					typeConfiguration
			);
		}
		else {
			sqlSelection = expression.createSqlSelection(
					jdbcPosition,
					valuesArrayPosition,
					javaType,
					virtual,
					typeConfiguration
			);
		}

		selectClause.addSqlSelection( sqlSelection );

		if ( selectionMap != null ) {
			if ( virtual ) {
				final SqlSelection[] selections;
				if ( existingSelection instanceof SqlSelection ) {
					selections = new SqlSelection[2];
					selections[0] = (SqlSelection) existingSelection;
				}
				else {
					final SqlSelection[] existingSelections = (SqlSelection[]) existingSelection;
					selections = new SqlSelection[existingSelections.length + 1];
					System.arraycopy( existingSelections, 0, selections, 0, existingSelections.length );
				}
				selections[selections.length - 1] = sqlSelection;
				selectionMap.put( expression, selections );
			}
			else {
				selectionMap.put( expression, sqlSelection );
			}
		}

		return sqlSelection;
	}
}
