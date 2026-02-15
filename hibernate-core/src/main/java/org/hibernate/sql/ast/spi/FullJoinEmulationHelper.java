/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.query.SortDirection;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;

import static java.util.Collections.emptyList;
import static org.hibernate.query.internal.NullPrecedenceHelper.isDefaultOrdering;
import static org.hibernate.sql.ast.spi.SqlAppender.COMMA_SEPARATOR;
import static org.hibernate.sql.ast.spi.SqlAppender.NO_SEPARATOR;
import static org.hibernate.sql.ast.tree.expression.SqlTupleContainer.getSqlTuple;
import static org.hibernate.sql.ast.tree.predicate.Predicate.combinePredicates;

/**
 * Emulates ANSI {@code FULL JOIN} using {@code UNION}.
 *
 * @author Gavin King
 */
@Internal
public final class FullJoinEmulationHelper {

	private final AbstractSqlAstTranslator<?> translator;
	private List<FullJoinEmulationInfo> fullJoinEmulationInfos;
	private List<FullJoinEmulationBranch> fullJoinEmulationBranches;
	private QueryGroup fullJoinEmulationQueryGroup;
	private List<Expression> fullJoinEmulationExtraSelections;
	private IdentityHashMap<SortSpecification, int[]> fullJoinEmulationNullPrecedenceSelectionIndexes;
	private boolean renderingFullJoinEmulationBranch;

	public FullJoinEmulationHelper(AbstractSqlAstTranslator<?> translator) {
		this.translator = translator;
	}

	public boolean renderFullJoinEmulationBranchIfNeeded(QuerySpec querySpec, Consumer<QuerySpec> renderQuerySpec) {
		final var branch = findFullJoinEmulationBranch( querySpec );
		final boolean hasBranch = branch != null;
		if ( hasBranch ) {
			renderFullJoinEmulationBranch( querySpec, branch.joinSides(), renderQuerySpec );
		}
		return hasBranch;
	}

	public boolean emulateFullJoinWithUnionIfNeeded(QuerySpec querySpec) {
		final var fullJoins = collectFullJoinEmulationInfos( querySpec );
		final boolean hasFullJoins = !fullJoins.isEmpty();
		if ( hasFullJoins ) {
			emulateFullJoinWithUnion( querySpec, fullJoins );
		}
		return hasFullJoins;
	}

	public boolean renderSelectClauseIfNeeded(SelectClause selectClause) {
		if ( renderingFullJoinEmulationBranch
				&& fullJoinEmulationExtraSelections != null ) {
			translator.getClauseStack().push( Clause.SELECT );
			try {
				translator.appendSql( "select " );
				if ( selectClause.isDistinct() ) {
					translator.appendSql( "distinct " );
				}
				translator.visitSqlSelections( selectClause );
				translator.renderVirtualSelections( selectClause );
				renderFullJoinEmulationAdditionalSelectItems( selectClause );
			}
			finally {
				translator.getClauseStack().pop();
			}
			return true;
		}
		return false;
	}

	private void renderFullJoinEmulationOrderBy(List<SortSpecification> sortSpecifications) {
		if ( sortSpecifications != null && !sortSpecifications.isEmpty() ) {
			translator.appendSql( " order by " );
			String separator = NO_SEPARATOR;
			for ( SortSpecification sortSpecification : sortSpecifications ) {
				translator.appendSql( separator );
				renderFullJoinEmulationSortSpecification( sortSpecification );
				separator = COMMA_SEPARATOR;
			}
		}
	}

	public boolean isFullJoinEmulationQueryPart(QueryPart queryPart) {
		return queryPart instanceof QuerySpec querySpec
			&& findFullJoinEmulationBranch( querySpec ) != null;
	}

	public boolean hasActiveFullJoinEmulation() {
		return fullJoinEmulationInfos != null;
	}

	public void renderOrderByIfNeeded(
			QueryPart queryPart,
			List<SortSpecification> sortSpecifications,
			Consumer<List<SortSpecification>> renderDefaultOrderBy) {
		if ( !isFullJoinEmulationQueryPart( queryPart ) ) {
			if ( isFullJoinEmulationOrderBy( queryPart, sortSpecifications ) ) {
				renderFullJoinEmulationOrderBy( sortSpecifications );
			}
			else {
				renderDefaultOrderBy.accept( sortSpecifications );
			}
		}
	}

	private boolean isFullJoinEmulationOrderBy(QueryPart queryPart, List<SortSpecification> sortSpecifications) {
		return fullJoinEmulationBranches != null
			&& ( queryPart == fullJoinEmulationQueryGroup
				// Some dialects (e.g. Sybase ASE) render ORDER BY outside the query group scope.
				|| fullJoinEmulationQueryGroup != null
					&& sortSpecifications == fullJoinEmulationQueryGroup.getSortSpecifications() );
	}

	private void emulateFullJoinWithUnion(QuerySpec querySpec, List<FullJoinEmulationInfo> fullJoins) {
		final int fullJoinCount = fullJoins.size();
		final int branchCount = 1 << fullJoinCount;
		final List<QueryPart> queryParts = new ArrayList<>( branchCount );
		final List<FullJoinEmulationBranch> branches = new ArrayList<>( branchCount );
		final var queryGroup =
				new QueryGroup( querySpec.isRoot(),
						SetOperator.UNION, queryParts );
		final var extraSelections =
				collectFullJoinEmulationExtraSelections( querySpec,
						countRenderedSelectItems( querySpec.getSelectClause() ) );
		if ( !extraSelections.isEmpty() ) {
			if ( querySpec.getSelectClause().isDistinct()
					|| !querySpec.getGroupByClauseExpressions().isEmpty()
					|| querySpec.getHavingClauseRestrictions() != null ) {
				throw new UnsupportedOperationException(
						"Full join emulation requires order by expressions to be in the select list for grouped or distinct queries"
				);
			}
			fullJoinEmulationExtraSelections = extraSelections;
		}
		copyOrderAndOffsetFetch( querySpec, queryGroup );

		collectFullJoinEmulationBranches(
				querySpec.isRoot()
						? querySpec
						: querySpec.asRootQuery(),
				fullJoins,
				0,
				new SqlAstJoinType[fullJoinCount],
				null,
				queryParts,
				branches
		);

		fullJoinEmulationInfos = fullJoins;
		fullJoinEmulationBranches = branches;
		fullJoinEmulationQueryGroup = queryGroup;
		try {
			queryGroup.accept( translator );
		}
		finally {
			fullJoinEmulationInfos = null;
			fullJoinEmulationBranches = null;
			fullJoinEmulationQueryGroup = null;
			fullJoinEmulationExtraSelections = null;
			fullJoinEmulationNullPrecedenceSelectionIndexes = null;
		}
	}

	private void collectFullJoinEmulationBranches(
			QuerySpec branchBase,
			List<FullJoinEmulationInfo> fullJoins,
			int index,
			SqlAstJoinType[] joinSides,
			Predicate predicate,
			List<QueryPart> queryParts,
			List<FullJoinEmulationBranch> branches) {
		if ( index == fullJoins.size() ) {
			final var branchQuery = branchBase.asSubQuery();
			clearSortAndOffsetFetch( branchQuery );
			if ( predicate != null ) {
				branchQuery.applyPredicate( predicate );
			}
			branches.add( new FullJoinEmulationBranch( branchQuery, joinSides.clone() ) );
			queryParts.add( branchQuery );
		}
		else {
			joinSides[index] = SqlAstJoinType.LEFT;
			collectFullJoinEmulationBranches(
					branchBase,
					fullJoins,
					index + 1,
					joinSides,
					predicate,
					queryParts,
					branches
			);

			joinSides[index] = SqlAstJoinType.RIGHT;
			collectFullJoinEmulationBranches(
					branchBase,
					fullJoins,
					index + 1,
					joinSides,
					combinePredicates( predicate,
							createTableGroupNullnessPredicate( fullJoins.get( index ).leftTableGroup() ) ),
					queryParts,
					branches
			);
		}
	}

	private static void clearSortAndOffsetFetch(QuerySpec querySpec) {
		final var sortSpecifications = querySpec.getSortSpecifications();
		if ( sortSpecifications != null ) {
			sortSpecifications.clear();
		}
		querySpec.setOffsetClauseExpression( null );
		querySpec.setFetchClauseExpression( null, null );
	}

	private void copyOrderAndOffsetFetch(QuerySpec source, QueryGroup target) {
		final var sortSpecifications = source.getSortSpecifications();
		if ( sortSpecifications != null ) {
			for ( var sortSpecification : sortSpecifications ) {
				target.addSortSpecification( sortSpecification );
			}
		}
		final var offsetClauseExpression = source.getOffsetClauseExpression();
		if ( offsetClauseExpression != null ) {
			target.setOffsetClauseExpression( offsetClauseExpression );
		}
		final var fetchClauseExpression = source.getFetchClauseExpression();
		if ( fetchClauseExpression != null ) {
			target.setFetchClauseExpression( fetchClauseExpression, source.getFetchClauseType() );
		}
	}

	private void renderFullJoinEmulationBranch(QuerySpec querySpec, SqlAstJoinType[] joinSides, Consumer<QuerySpec> renderQuerySpec) {
		applyFullJoinJoinType( joinSides );
		final boolean previousRenderingBranch = renderingFullJoinEmulationBranch;
		renderingFullJoinEmulationBranch = true;
		try {
			renderQuerySpec.accept( querySpec );
		}
		finally {
			renderingFullJoinEmulationBranch = previousRenderingBranch;
			restoreFullJoinJoinType();
		}
	}

	private void applyFullJoinJoinType(SqlAstJoinType[] joinTypes) {
		for ( int i = 0; i < fullJoinEmulationInfos.size(); i++ ) {
			fullJoinEmulationInfos.get( i ).join().setJoinType( joinTypes[i] );
		}
	}

	private void restoreFullJoinJoinType() {
		for ( var info : fullJoinEmulationInfos ) {
			info.join().setJoinType( info.originalJoinType() );
		}
	}

	private void collectFullJoinEmulationInfos(TableGroup tableGroup, List<FullJoinEmulationInfo> fullJoins) {
		collectFullJoinEmulationInfos( tableGroup, tableGroup.getTableGroupJoins(), fullJoins );
		collectFullJoinEmulationInfos( tableGroup, tableGroup.getNestedTableGroupJoins(), fullJoins );
	}

	private List<FullJoinEmulationInfo> collectFullJoinEmulationInfos(QuerySpec querySpec) {
		final var fromClause = querySpec.getFromClause();
		if ( fromClause == null || fromClause.getRoots().isEmpty() ) {
			return emptyList();
		}
		else {
			final List<FullJoinEmulationInfo> fullJoins = new ArrayList<>();
			for ( var root : fromClause.getRoots() ) {
				collectFullJoinEmulationInfos( root, fullJoins );
			}
			return fullJoins;
		}
	}

	private void collectFullJoinEmulationInfos(
			TableGroup leftTableGroup,
			List<TableGroupJoin> joins,
			List<FullJoinEmulationInfo> fullJoins) {
		for ( var join : joins ) {
			if ( join.getJoinType() == SqlAstJoinType.FULL ) {
				fullJoins.add( new FullJoinEmulationInfo( leftTableGroup, join ) );
			}
			final var joinedGroup = join.getJoinedGroup();
			if ( joinedGroup != null ) {
				collectFullJoinEmulationInfos( joinedGroup, fullJoins );
			}
		}
	}

	private Predicate createTableGroupNullnessPredicate(TableGroup tableGroup) {
		return createValuedModelPartNullnessPredicate( tableGroup,
				getValuedModelPart( tableGroup.getModelPart() ) );
	}

	private static ValuedModelPart getValuedModelPart(ModelPartContainer modelPart) {
		if ( modelPart instanceof EntityMappingType mappingType ) {
			return mappingType.getIdentifierMapping();
		}
		else if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			return entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return pluralAttributeMapping.getKeyDescriptor();
		}
		else if ( modelPart instanceof ValuedModelPart directValuedModelPart ) {
			return directValuedModelPart;
		}
		else {
			throw new UnsupportedOperationException(
					"Full join emulation requires a table group with selectable columns but got: "
					+ modelPart.getPartName()
			);
		}
	}

	private Predicate createValuedModelPartNullnessPredicate(TableGroup tableGroup, ValuedModelPart valuedModelPart) {
		final List<ColumnReference> columnReferences = new ArrayList<>( valuedModelPart.getJdbcTypeCount() );
		valuedModelPart.forEachSelectable( (selectionIndex, selectableMapping) -> {
			final var tableReference = tableGroup.resolveTableReference(
					tableGroup.getNavigablePath(),
					valuedModelPart,
					selectableMapping.getContainingTableExpression()
			);
			columnReferences.add( new ColumnReference( tableReference, selectableMapping ) );
		} );

		return new NullnessPredicate( columnReferences.size() == 1
				? columnReferences.get( 0 )
				: new SqlTuple( columnReferences, valuedModelPart ) );
	}

	private FullJoinEmulationBranch findFullJoinEmulationBranch(QuerySpec querySpec) {
		if ( fullJoinEmulationBranches != null ) {
			for ( var branch : fullJoinEmulationBranches ) {
				if ( branch.querySpec() == querySpec ) {
					return branch;
				}
			}
		}
		return null;
	}

	private void renderFullJoinEmulationSortSpecification(SortSpecification sortSpecification) {
		final var tuple = getSqlTuple( sortSpecification.getSortExpression() );
		final int[] sortSelectionIndexes = sortSpecification.getSortSelectionIndexes();
		final int[] nullPrecedenceSelectionIndexes =
				fullJoinEmulationNullPrecedenceSelectionIndexes == null
						? null
						: fullJoinEmulationNullPrecedenceSelectionIndexes.get( sortSpecification );
		final int count = tuple == null ? 1 : tuple.getExpressions().size();
		String separator = NO_SEPARATOR;
		for ( int i = 0; i < count; i++ ) {
			translator.appendSql( separator );
			renderFullJoinEmulationSortExpression(
					sortSpecification,
					sortSelectionIndexes[i],
					nullPrecedenceSelectionIndexes == null ? -1 : nullPrecedenceSelectionIndexes[i]
			);
			separator = COMMA_SEPARATOR;
		}
	}

	private void renderFullJoinEmulationSortExpression(
			SortSpecification sortSpecification,
			int sortSelectionIndex,
			int nullPrecedenceSelectionIndex) {
		if ( sortSpecification.isIgnoreCase() ) {
			throw new UnsupportedOperationException( "Full join emulation does not support ignore case ordering for union queries" );
		}
		if ( sortSelectionIndex < 0 ) {
			throw new UnsupportedOperationException( "Full join emulation requires order by expressions to be in the select list for union queries" );
		}

		final var sortOrder = sortSpecification.getSortOrder();
		final var nullPrecedence = resolveRenderedNullPrecedence( sortSpecification );
		final boolean renderNullPrecedence = nullPrecedence != null;
		final boolean supportsNullPrecedence = renderNullPrecedence && translator.getDialect().supportsNullPrecedence();
		if ( renderNullPrecedence && !supportsNullPrecedence ) {
			if ( nullPrecedenceSelectionIndex < 0 ) {
				throw new IllegalStateException( "Full join emulation null precedence index is missing for union query ordering" );
			}
			translator.appendSql( nullPrecedenceSelectionIndex + 1 );
			translator.appendSql( COMMA_SEPARATOR );
		}

		translator.appendSql( sortSelectionIndex + 1 );

		if ( sortOrder == SortDirection.DESCENDING ) {
			translator.appendSql( " desc" );
		}
		else if ( sortOrder == SortDirection.ASCENDING && renderNullPrecedence && supportsNullPrecedence ) {
			translator.appendSql( " asc" );
		}

		if ( renderNullPrecedence && supportsNullPrecedence ) {
			translator.appendSql( " nulls " );
			translator.appendSql( nullPrecedence == Nulls.LAST ? "last" : "first" );
		}
	}

	private void renderFullJoinEmulationAdditionalSelectItems(SelectClause selectClause) {
		String separator =
				countRenderedSelectItems( selectClause ) > 0
						? COMMA_SEPARATOR
						: NO_SEPARATOR;
		for ( var extraExpression : fullJoinEmulationExtraSelections ) {
			translator.appendSql( separator );
			translator.renderSelectExpression( extraExpression );
			separator = COMMA_SEPARATOR;
		}
	}

	public static int countRenderedSelectItems(SelectClause selectClause) {
		int count = 0;
		for ( var sqlSelection : selectClause.getSqlSelections() ) {
			if ( !sqlSelection.isVirtual() ) {
				final var tuple = getSqlTuple( sqlSelection.getExpression() );
				if ( tuple != null ) {
					count += tuple.getExpressions().size();
				}
				else {
					count++;
				}
			}
		}
		return count;
	}

	private List<Expression> collectFullJoinEmulationExtraSelections(QuerySpec querySpec, int selectItemCount) {
		final var sortSpecifications = querySpec.getSortSpecifications();
		if ( sortSpecifications == null || sortSpecifications.isEmpty() ) {
			return emptyList();
		}
		else {
			final List<Expression> extraSelections = new ArrayList<>();
			IdentityHashMap<SortSpecification, int[]> nullPrecedenceSelectionIndexes = null;
			for ( var sortSpecification : sortSpecifications ) {
				resolveSortSelectionIndexes( sortSpecification, selectItemCount, extraSelections );
				final var nullPrecedence = resolveRenderedNullPrecedence( sortSpecification );
				if ( nullPrecedence != null && !translator.getDialect().supportsNullPrecedence() ) {
					if ( nullPrecedenceSelectionIndexes == null ) {
						nullPrecedenceSelectionIndexes = new IdentityHashMap<>();
					}
					resolveSortNullPrecedenceSelectionIndexes(
							sortSpecification,
							nullPrecedence,
							selectItemCount,
							extraSelections,
							nullPrecedenceSelectionIndexes
					);
				}
			}
			fullJoinEmulationNullPrecedenceSelectionIndexes = nullPrecedenceSelectionIndexes;
			return extraSelections;
		}
	}

	private void resolveSortSelectionIndexes(
			SortSpecification sortSpecification,
			int selectItemCount,
			List<Expression> extraSelections) {
		final int[] sortSelectionIndexes = sortSpecification.getSortSelectionIndexes();
		final var expressions = getSortExpressions( sortSpecification );
		for ( int i = 0; i < expressions.size(); i++ ) {
			if ( sortSelectionIndexes[i] < 0 ) {
				extraSelections.add( expressions.get( i ) );
				sortSelectionIndexes[i] = selectItemCount + extraSelections.size() - 1;
			}
		}
	}

	private void resolveSortNullPrecedenceSelectionIndexes(
			SortSpecification sortSpecification,
			Nulls nullPrecedence,
			int selectItemCount,
			List<Expression> extraSelections,
			IdentityHashMap<SortSpecification, int[]> nullPrecedenceSelectionIndexes) {
		final var expressions = getSortExpressions( sortSpecification );
		final int[] indexes = new int[expressions.size()];
		for ( int i = 0; i < expressions.size(); i++ ) {
			extraSelections.add( createSortNullPrecedenceEmulationExpression( expressions.get( i ), nullPrecedence ) );
			indexes[i] = selectItemCount + extraSelections.size() - 1;
		}
		nullPrecedenceSelectionIndexes.put( sortSpecification, indexes );
	}

	private List<? extends Expression> getSortExpressions(SortSpecification sortSpecification) {
		final var sortExpression = sortSpecification.getSortExpression();
		final int[] sortSelectionIndexes = sortSpecification.getSortSelectionIndexes();
		if ( sortSelectionIndexes == null ) {
			throw new IllegalStateException( "SortSpecification is missing selection indexes" );
		}
		final var tuple = getSqlTuple( sortExpression );
		final var expressions = tuple == null ? List.of( sortExpression ) : tuple.getExpressions();
		if ( sortSelectionIndexes.length != expressions.size() ) {
			throw new IllegalStateException( "SortSpecification selection indexes size mismatch" );
		}
		return expressions;
	}

	private Nulls resolveRenderedNullPrecedence(SortSpecification sortSpecification) {
		final var sortOrder = sortSpecification.getSortOrder();
		Nulls nullPrecedence = sortSpecification.getNullPrecedence();
		if ( nullPrecedence == null || nullPrecedence == Nulls.NONE ) {
			nullPrecedence =
					translator.getSessionFactory().getSessionFactoryOptions()
							.getDefaultNullPrecedence();
		}
		if ( nullPrecedence == null
				|| isDefaultOrdering( nullPrecedence, sortOrder,
						translator.getDialect().getNullOrdering() ) ) {
			return null;
		}
		return nullPrecedence;
	}

	private Expression createSortNullPrecedenceEmulationExpression(Expression sortExpression, Nulls nullPrecedence) {
		final var integerType = translator.getIntegerType();
		return new CaseSearchedExpression(
				integerType,
				List.of( new CaseSearchedExpression.WhenFragment(
						new NullnessPredicate( sortExpression ),
						new QueryLiteral<>( nullPrecedence == Nulls.FIRST ? 0 : 1, integerType )
				) ),
				new QueryLiteral<>( nullPrecedence == Nulls.FIRST ? 1 : 0, integerType )
		);
	}

	private record FullJoinEmulationInfo(TableGroup leftTableGroup, TableGroupJoin join, SqlAstJoinType originalJoinType) {
		FullJoinEmulationInfo(TableGroup leftTableGroup, TableGroupJoin join) {
			this( leftTableGroup, join, join.getJoinType() );
		}
	}

	private record FullJoinEmulationBranch(QuerySpec querySpec, SqlAstJoinType[] joinSides) {}
}
