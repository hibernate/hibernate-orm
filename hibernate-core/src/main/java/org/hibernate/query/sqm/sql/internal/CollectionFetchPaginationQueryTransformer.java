/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl.CaseStatementDiscriminatorExpression;
import org.hibernate.query.SortDirection;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.ExpressionReplacementWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.results.internal.ResolvedSqlSelection;
import org.hibernate.type.BasicType;

import static java.util.Collections.emptyList;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;
import static org.hibernate.sql.ast.SqlAstJoinType.*;

/**
 * Deals with many-valued join fetches in a query with pagaination or a limit.
 * <p>
 * Pushes the offset/fetch of a top-level {@code QuerySpec} down into a derived
 * table over the root entity, leaving fetch joins of plural attributes on the
 * outer query so the limit applies to parent rows rather than to the cartesian
 * product of root and collection rows.
 * <p>
 * In the simplest case, given an HQL query
 * <pre>
 *     from Book b left join fetch b.authors where b.year = :y order by b.isbn
 * </pre>
 * with {@code setMaxResults(n)}, the resulting SQL is
 * <pre>
 *     select ... from (select b.&#42; from Book b
 *                          where b.year = ? order by b.isbn limit ?) b
 *                 left join Author a on a.book_isbn = b.isbn
 *                 order by b.isbn
 * </pre>
 * The transformer reuses the original primary root's primary table alias as the
 * derived table identification variable, so existing column references in the
 * outer that point at the primary root resolve through it without rewriting.
 * <p>
 * For more complex queries — multiple roots, non-fetch joins referenced in the
 * outer SELECT or ORDER BY — the inner derived table also has to expose those
 * additional aliases' columns. Those aliases are <em>absorbed</em>: their
 * referenced columns are projected from the inner with prefixed names, the
 * derived table column-list aliases them, and a walker rewrites every outer
 * {@code ColumnReference} that pointed at an absorbed alias to instead address
 * the derived table.
 * <p>
 * Predicates and order-by elements may not reference fetched collection
 * elements (an HQL constraint we depend on here).
 *
 * @author Gavin King
 */
public class CollectionFetchPaginationQueryTransformer implements QueryTransformer {
	private final boolean groupRowsByOwnerForScroll;

	public CollectionFetchPaginationQueryTransformer(boolean groupRowsByOwnerForScroll) {
		this.groupRowsByOwnerForScroll = groupRowsByOwnerForScroll;
	}

	@Override
	public QuerySpec transform(
			CteContainer cteContainer,
			QuerySpec querySpec,
			SqmToSqlAstConverter converter) {
		final var roots = querySpec.getFromClause().getRoots();
		if ( roots.isEmpty() ) {
			return querySpec;
		}
		else {
			final var primaryRoot = primaryRoot( roots );
			if ( primaryRoot instanceof AbstractTableGroup
					&& primaryRoot.getModelPart() instanceof EntityMappingType primaryEntity
					&& primaryRoot.getPrimaryTableReference() instanceof NamedTableReference primaryNamed ) {
				return transform( querySpec, converter, primaryEntity, primaryNamed, primaryRoot );
			}
			else {
				return querySpec;
			}
		}
	}

	private QuerySpec transform(
			QuerySpec querySpec,
			SqmToSqlAstConverter converter,
			EntityMappingType primaryEntity,
			NamedTableReference primaryTableReference,
			TableGroup primaryRoot) {
		final String primaryAlias = primaryTableReference.getIdentificationVariable();
		final String primaryTableExpr = primaryTableReference.getTableExpression();

		final var roots = querySpec.getFromClause().getRoots();
		final var movedJoins = collectMovedJoins( primaryRoot );
		// Aliases that will still be directly reachable from the outer query after
		// the plural fetch joins are moved there. Anything else referenced by the
		// outer query has to be projected through the derived table first.
		final var outerAliases = collectOuterAliases( primaryAlias, movedJoins );
		final var absorption = collectAbsorption( querySpec, roots, movedJoins, outerAliases );
		if ( absorption == null ) {
			// If we discovered a column reference that does not belong to either the
			// future outer query or the remaining inner query, this transformer can't
			// safely rewrite the shape. Leave the query unchanged in that case.
			return querySpec;
		}
		else {
			applyInnerJoinFetchPredicates( querySpec, movedJoins, converter );
			// The inner SELECT is rebuilt from scratch, but the outer query still has to
			// project the original domain results. Capture them before the inner rewrite.
			final var originalSelections =
					new ArrayList<>( querySpec.getSelectClause().getSqlSelections() );
			final var columnNames =
					rebuildInnerSelect( querySpec, primaryEntity, primaryTableExpr, primaryAlias, absorption );
			detachMovedJoins( movedJoins );
			final var originalSortSpecifications =
					prepareInnerSortSpecifications( querySpec, outerAliases, primaryAlias );
			final var inner = querySpec.asSubQuery();
			final var outer = new QuerySpec( querySpec.isRoot(), 1 );
			final var derivedRoot = createDerivedRoot(
					converter,
					primaryRoot,
					primaryAlias,
					primaryTableExpr,
					columnNames,
					inner
			);
			outer.getFromClause().addRoot( derivedRoot );
			final var rewriter = new ColumnReferenceRewriter( absorption, primaryAlias );
			reattachMovedJoins( derivedRoot, movedJoins, rewriter );
			addOuterSelections( outer, originalSelections, rewriter );
			addOuterSortSpecifications(
					outer,
					originalSortSpecifications,
					outerAliases,
					primaryAlias,
					primaryEntity,
					rewriter
			);
			return outer;
		}
	}

	private static List<MovedJoin> collectMovedJoins(TableGroup primaryRoot) {
		// Only plural fetched joins move to the outer — they are the
		// cartesian-product source we're trying to escape. Fetched singular
		// joins stay inside the inner so their columns can drive the
		// pagination's ORDER BY (e.g. {@code order by p.name} where {@code p}
		// is a fetched {@code @ManyToOne}); their columns get absorbed into
		// the derived table for the outer SELECT to reach. Plural fetches
		// nested under fetched singulars are also moved out — the parent
		// singular's table reference stays inner, and the plural's join
		// predicate gets absorbed-rewritten through the derived alias.
		final List<MovedJoin> movedJoins = new ArrayList<>();
		collectMovedPluralFetches( primaryRoot, movedJoins );
		return movedJoins;
	}

	private static Set<String> collectOuterAliases(String primaryAlias, List<MovedJoin> movedJoins) {
		final Set<String> outerAliases = new HashSet<>();
		outerAliases.add( primaryAlias );
		for ( var moved : movedJoins ) {
			collectAliases( moved.join().getJoinedGroup(), outerAliases );
		}
		return outerAliases;
	}

	private static @Nullable Map<AbsorbedKey, AbsorbedColumn> collectAbsorption(
			QuerySpec querySpec,
			List<TableGroup> roots,
			List<MovedJoin> movedJoins,
			Set<String> outerAliases) {
		final Map<AbsorbedKey, AbsorbedColumn> absorption = new LinkedHashMap<>();
		final var collector = new AbsorbedColumnCollector( outerAliases, absorption );
		collectAbsorbedReferences( querySpec, movedJoins, collector );
		return allAbsorbedAliasesRemainInner( roots, absorption ) ? absorption : null;
	}

	private static void collectAbsorbedReferences(
			QuerySpec querySpec,
			List<MovedJoin> movedJoins,
			AbsorbedColumnCollector collector) {
		// Walk the original outer content for any ColumnReference whose qualifier
		// is neither the primary alias nor inside a moved-out join. Those qualifiers
		// belong to TableGroups that stay in the inner — they need to be "absorbed"
		// into the derived table.
		for ( var selection : querySpec.getSelectClause().getSqlSelections() ) {
			selection.getExpression().accept( collector );
		}
		if ( querySpec.hasSortSpecifications() ) {
			for ( var sort : querySpec.getSortSpecifications() ) {
				sort.getSortExpression().accept( collector );
			}
		}
		// Moved fetch joins' predicates reference the join target (the parent
		// alias) plus the moved alias itself. For direct moves the parent is
		// the primary alias, and no rewriting is needed; for nested moves the
		// parent is a fetched singular's alias that is inner-only and gets
		// absorbed into the derived table here.
		for ( var moved : movedJoins ) {
			final var predicate = moved.join().getPredicate();
			if ( predicate != null ) {
				predicate.accept( collector );
			}
		}
	}

	private static boolean allAbsorbedAliasesRemainInner(
			List<TableGroup> roots,
			Map<AbsorbedKey, AbsorbedColumn> absorption) {
		final Set<String> innerAliases = new HashSet<>();
		for ( var root : roots ) {
			collectAliases( root, innerAliases );
		}
		// the primary alias is in both - that's fine
		for ( var key : absorption.keySet() ) {
			if ( !innerAliases.contains( key.qualifier() ) ) {
				// The rewriter only knows how to remap inner-only aliases through the
				// derived table. If an absorbed qualifier is not even part of the inner
				// query tree, this query shape is outside what this transformer supports.
				return false;
			}
		}
		return true;
	}

	private static void applyInnerJoinFetchPredicates(
			QuerySpec querySpec,
			List<MovedJoin> movedJoins,
			SqmToSqlAstConverter converter) {
		// Inner join fetches act as a filter on the parent rows (only parents that
		// have at least one matching child appear). To keep that semantics when the
		// fetch join moves to the outer, add an EXISTS predicate to the inner
		// referencing the joined group, so the inner pagination only sees parents
		// that would have matched the inner join.
		final var typeConfiguration = converter.getCreationContext().getTypeConfiguration();
		final var integerType = typeConfiguration.getBasicTypeForJavaType( Integer.class );
		final var booleanType = typeConfiguration.getBasicTypeForJavaType( Boolean.class );
		assert integerType != null;
		for ( var moved : movedJoins ) {
			final var join = moved.join();
			if ( join.getJoinType() == INNER && join.getPredicate() != null ) {
				final var existsSpec = new QuerySpec( false, 1 );
				existsSpec.getFromClause().addRoot( existsRootTableGroup( join ) );
				existsSpec.getSelectClause().addSqlSelection(
						new ResolvedSqlSelection(
								0,
								new QueryLiteral<>( 1, integerType ),
								integerType
						)
				);
				existsSpec.applyPredicate( join.getPredicate() );
				querySpec.applyPredicate(
						new ExistsPredicate(
								new SelectStatement( existsSpec ),
								false,
								booleanType
						)
				);
			}
		}
	}

	private static TableGroup existsRootTableGroup(TableGroupJoin join) {
		final var joinedGroup = join.getJoinedGroup();
		if ( joinedGroup instanceof CollectionTableGroup collectionTableGroup
				&& canUseCollectionTableOnlyExists( collectionTableGroup, join.getPredicate() ) ) {
			return createCollectionTableOnlyGroup( collectionTableGroup );
		}
		else {
			return joinedGroup;
		}
	}

	private static boolean canUseCollectionTableOnlyExists(
			CollectionTableGroup collectionTableGroup,
			org.hibernate.sql.ast.tree.predicate.Predicate joinPredicate) {
		final var pluralAttribute = collectionTableGroup.getModelPart();
		if ( !pluralAttribute.getCollectionDescriptor().isManyToMany() ) {
			return false;
		}
		else {
			final Set<String> nestedAliases = new HashSet<>();
			collectJoinedAliases( collectionTableGroup, nestedAliases );
			return !sqlAstReferencesAnyAlias( joinPredicate, nestedAliases, null )
				&& hasOnlySimpleJoinPredicates( collectionTableGroup );
		}
	}

	private static void collectJoinedAliases(TableGroup group, Set<String> aliases) {
		for ( var tableGroupJoin : group.getTableGroupJoins() ) {
			collectAliases( tableGroupJoin.getJoinedGroup(), aliases );
		}
		for ( var tableGroupJoin : group.getNestedTableGroupJoins() ) {
			collectAliases( tableGroupJoin.getJoinedGroup(), aliases );
		}
	}

	private static boolean hasOnlySimpleJoinPredicates(TableGroup group) {
		for ( var join : group.getTableGroupJoins() ) {
			if ( !hasOnlySimpleJoinPredicates( join ) ) {
				return false;
			}
		}
		for ( var join : group.getNestedTableGroupJoins() ) {
			if ( !hasOnlySimpleJoinPredicates( join ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasOnlySimpleJoinPredicates(TableGroupJoin join) {
		final var predicate = join.getPredicate();
		if ( predicate != null ) {
			final var modelPart = join.getJoinedGroup().getModelPart();
			if ( !(modelPart instanceof TableGroupJoinProducer joinProducer)
					|| !joinProducer.isSimpleJoinPredicate( predicate ) ) {
				return false;
			}
		}
		return hasOnlySimpleJoinPredicates( join.getJoinedGroup() );
	}

	private static CollectionTableGroup createCollectionTableOnlyGroup(CollectionTableGroup collectionTableGroup) {
		final var strippedGroup = new CollectionTableGroup(
				collectionTableGroup.canUseInnerJoins(),
				collectionTableGroup.getNavigablePath(),
				collectionTableGroup.getModelPart(),
				collectionTableGroup.isFetched(),
				collectionTableGroup.getSourceAlias(),
				collectionTableGroup.getPrimaryTableReference(),
				true,
				collectionTableGroup.getSqlAliasBase(),
				s -> false,
				null,
				collectionTableGroup.getModelPart().getCollectionDescriptor().getFactory()
		);
		for ( var tableReferenceJoin : collectionTableGroup.getTableReferenceJoins() ) {
			strippedGroup.addTableReferenceJoin( tableReferenceJoin );
		}
		return strippedGroup;
	}

	private static List<String> rebuildInnerSelect(
			QuerySpec querySpec,
			EntityMappingType primaryEntity,
			String primaryTableExpr,
			String primaryAlias,
			Map<AbsorbedKey, AbsorbedColumn> absorption) {
		// Rebuild inner SELECT: primary entity's primary table columns (kept under their
		// original names so outer refs to the primary alias resolve naturally) and
		// absorbed columns (renamed to <alias>_<col> in the derived column list).
		// The returned columnNames list must stay in lockstep with the inner SELECT
		// projection order because QueryPartTableGroup uses it as the derived table's
		// explicit column list.
		final List<String> columnNames = new ArrayList<>();
		querySpec.getSelectClause().getSqlSelections().clear();
		final int nextPosition =
				addPrimaryTableSelections( querySpec, primaryEntity, primaryTableExpr, primaryAlias, columnNames );
		addAbsorbedSelections( querySpec, absorption, columnNames, nextPosition );
		return columnNames;
	}

	private static int addPrimaryTableSelections(
			QuerySpec querySpec,
			EntityMappingType primaryEntity,
			String primaryTableExpr,
			String primaryAlias,
			List<String> columnNames) {
		final Set<String> seen = new HashSet<>();
		// Joined inheritance with no @DiscriminatorColumn synthesizes a CASE
		// expression on the subtype tables and exposes it through a placeholder
		// selectable DISCRIMINATOR_ROLE_NAME. The CASE itself is in the outer
		// SELECT and AbsorbedColumnCollector picks up its component column refs,
		// so the inner doesn't need (and can't render) this placeholder.
		seen.add( DISCRIMINATOR_ROLE_NAME );
		class Projector implements SelectableConsumer {
			int position;
			@Override
			public void accept(int idx, SelectableMapping selectable) {
				if ( primaryTableExpr.equals( selectable.getContainingTableExpression() ) ) {
					final String columnName = selectable.getSelectionExpression();
					if ( seen.add( columnName ) ) {
						querySpec.getSelectClause().addSqlSelection(
								new ResolvedSqlSelection(
										position++,
										new ColumnReference( primaryAlias, selectable ),
										(BasicType<?>) selectable.getJdbcMapping()
								)
						);
						columnNames.add( columnName );
					}
				}
			}
		}
		final var projector = new Projector();
		primaryEntity.getIdentifierMapping().forEachSelectable( projector );
		final var discriminatorMapping = primaryEntity.getDiscriminatorMapping();
		if ( discriminatorMapping != null ) {
			discriminatorMapping.forEachSelectable( projector );
		}
		final var versionMapping = primaryEntity.getVersionMapping();
		if ( versionMapping != null ) {
			versionMapping.forEachSelectable( projector );
		}
		primaryEntity.forEachSelectable( projector );
		return projector.position;
	}

	private static void addAbsorbedSelections(
			QuerySpec querySpec,
			Map<AbsorbedKey, AbsorbedColumn> absorption,
			List<String> columnNames,
			int startPosition) {
		int position = startPosition;
		for ( var entry : absorption.entrySet() ) {
			final var absorbedKey = entry.getKey();
			final var absorbedColumn = entry.getValue();
			final String exposedName = absorbedKey.qualifier() + "_" + absorbedKey.columnName();
			// Expose the absorbed column under a stable derived-table column name and
			// remember that name so outer ColumnReferences can be rewritten to it.
			querySpec.getSelectClause().addSqlSelection(
					new ResolvedSqlSelection(
							position++,
							new ColumnReference(
									absorbedKey.qualifier(),
									absorbedKey.columnName(),
									false,
									null,
									absorbedColumn.jdbcMapping
							),
							(BasicType<?>) absorbedColumn.jdbcMapping
					)
			);
			columnNames.add( exposedName );
			absorbedColumn.exposedName = exposedName;
		}
	}

	private static void detachMovedJoins(List<MovedJoin> movedJoins) {
		// Detach the moved (fetched) plural joins from whichever group they were
		// attached to in the inner — the primary root for direct fetches, or a
		// fetched-singular's group for nested fetches.
		for ( var moved : movedJoins ) {
			moved.parent().removeTableGroupJoin( moved.join() );
		}
	}

	private static List<SortSpecification> prepareInnerSortSpecifications(
			QuerySpec querySpec,
			Set<String> outerAliases,
			String primaryAlias) {
		// Split sort specs: those that reference an alias that's now in the outer
		// (e.g. the synthetic ORDER BY Hibernate adds on a fetched collection's FK)
		// can't stay on the inner — they'd reference a TableGroup we just removed.
		// We keep all original sort specs to apply on the outer; only the
		// inner-resolvable ones stay in the inner for deterministic LIMIT.
		if ( !querySpec.hasSortSpecifications() ) {
			return emptyList();
		}
		else {
			final var sortSpecifications = querySpec.getSortSpecifications();
			final var originalSortSpecifications = new ArrayList<>( sortSpecifications );
			final List<SortSpecification> innerSortSpecifications =
					new ArrayList<>( originalSortSpecifications.size() );
			for ( var sort : originalSortSpecifications ) {
				if ( !referencesMovedAlias( sort, outerAliases, primaryAlias ) ) {
					innerSortSpecifications.add( sort );
				}
			}
			sortSpecifications.clear();
			sortSpecifications.addAll( innerSortSpecifications );
			return originalSortSpecifications;
		}
	}

	private static QueryPartTableGroup createDerivedRoot(
			SqmToSqlAstConverter converter,
			TableGroup primaryRoot,
			String primaryAlias,
			String primaryTableExpr,
			List<String> columnNames,
			QuerySpec inner) {
		// Reuse the original root alias as the derived-table alias so existing outer
		// references to the root can keep using that qualifier unchanged.
		return new QueryPartTableGroup(
				primaryRoot.getNavigablePath(),
				null,
				new SelectStatement( inner ),
				primaryAlias,
				columnNames,
				new HashSet<>( Set.of( primaryTableExpr ) ),
				false,
				true,
				converter.getLoadQueryInfluencers().getSessionFactory()
		);
	}

	private static void reattachMovedJoins(
			QueryPartTableGroup derivedRoot,
			List<MovedJoin> movedJoins,
			ColumnReferenceRewriter rewriter) {
		// Add outer SELECT items and reattach moved fetch joins, rewriting any
		// absorbed-alias ColumnReferences.
		for ( var moved : movedJoins ) {
			final var join = moved.join();
			final var originalPredicate = join.getPredicate();
			if ( originalPredicate == null ) {
				derivedRoot.addTableGroupJoin( join );
			}
			else {
				final var rewrittenPredicate = rewriter.replaceExpressions( originalPredicate );
				if ( rewrittenPredicate == originalPredicate ) {
					derivedRoot.addTableGroupJoin( join );
				}
				else {
					// Predicates carry a TableGroup reference at construction time.
					// Rebuild the join with the rewritten predicate so the outer's
					// JOIN ON addresses derived-alias columns (e.g. for subclass
					// roots where the parent FK references a now-absorbed super
					// table's PK, or for nested fetches where the predicate
					// references the parent fetched singular's alias).
					derivedRoot.addTableGroupJoin( new TableGroupJoin(
							join.getNavigablePath(),
							join.getJoinType(),
							join.getJoinedGroup(),
							rewrittenPredicate
					) );
				}
			}
		}
	}

	private static void addOuterSelections(
			QuerySpec outer,
			List<SqlSelection> originalSelections,
			ColumnReferenceRewriter rewriter) {
		for ( var selection : originalSelections ) {
			final var expression = selection.getExpression();
			final var rewritten = rewriter.rewrite( expression );
			// Most original select expressions can be reused as-is because the derived
			// table keeps the root alias. Only expressions that touched absorbed aliases
			// need to be rebuilt against the derived-table column names.
			outer.getSelectClause()
					.addSqlSelection( rewritten == expression
							? selection
							: new ResolvedSqlSelection(
									selection.getValuesArrayPosition(),
									rewritten,
									(BasicType<?>)
											rewritten.getExpressionType()
												.getSingleJdbcMapping()
							)
					);
		}
	}

	private void addOuterSortSpecifications(
			QuerySpec outer,
			List<SortSpecification> originalSortSpecifications,
			Set<String> outerAliases,
			String primaryAlias,
			EntityMappingType primaryEntity,
			ColumnReferenceRewriter rewriter) {
		// Scroll()/getResultStream() groups fetched rows into a logical result only
		// while the root entity key stays consecutive. When collection ordering
		// moved to the outer query (for example, via @OrderBy on the fetched
		// collection), sorting only by child columns can interleave rows from
		// different parents. Stabilize the outer row order by inserting the root
		// identifier between the root-level ordering and the moved collection
		// ordering. List execution does not need this.
		if ( groupRowsByOwnerForScroll ) {
			addRootOuterSortSpecifications(
					outer,
					originalSortSpecifications,
					outerAliases,
					primaryAlias,
					rewriter
			);
			addOwnerIdentifierSortSpecifications( outer, primaryAlias, primaryEntity );
			addMovedAliasOuterSortSpecifications(
					outer,
					originalSortSpecifications,
					outerAliases,
					primaryAlias,
					rewriter
			);
		}
		else {
			// Outer ORDER BY mirrors the original (full) sort specs, with absorbed
			// column references rewritten through the derived alias.
			for ( var sort : originalSortSpecifications ) {
				addOuterSortSpecification( outer, sort, rewriter );
			}
		}
	}

	private static void addRootOuterSortSpecifications(
			QuerySpec outer,
			List<SortSpecification> originalSortSpecifications,
			Set<String> outerAliases,
			String primaryAlias,
			ColumnReferenceRewriter rewriter) {
		for ( var sort : originalSortSpecifications ) {
			if ( !referencesMovedAlias( sort, outerAliases, primaryAlias ) ) {
				addOuterSortSpecification( outer, sort, rewriter );
			}
		}
	}

	private static void addOwnerIdentifierSortSpecifications(
			QuerySpec outer,
			String primaryAlias,
			EntityMappingType primaryEntity) {
		primaryEntity.getIdentifierMapping().forEachSelectable( (selectionIndex, selectableMapping) -> {
			final var identifierColumn = new ColumnReference( primaryAlias, selectableMapping );
			if ( !hasSortExpression( outer.getSortSpecifications(), identifierColumn ) ) {
				outer.addSortSpecification( new SortSpecification( identifierColumn, SortDirection.ASCENDING ) );
			}
		} );
	}

	private static void addMovedAliasOuterSortSpecifications(
			QuerySpec outer,
			List<SortSpecification> originalSortSpecifications,
			Set<String> outerAliases,
			String primaryAlias,
			ColumnReferenceRewriter rewriter) {
		for ( var sort : originalSortSpecifications ) {
			if ( referencesMovedAlias( sort, outerAliases, primaryAlias ) ) {
				addOuterSortSpecification( outer, sort, rewriter );
			}
		}
	}

	private static boolean referencesMovedAlias(
			SortSpecification sortSpecification,
			Set<String> outerAliases,
			String primaryAlias) {
		// outerAliases contains the aliases introduced by moved fetch joins. If a sort
		// expression touches one of them, it cannot stay on the inner query after the
		// join is detached there.
		return sqlAstReferencesAnyAlias( sortSpecification.getSortExpression(), outerAliases, primaryAlias );
	}

	private static void addOuterSortSpecification(
			QuerySpec outer,
			SortSpecification sort,
			ColumnReferenceRewriter rewriter) {
		final var sortExpression = sort.getSortExpression();
		final var rewritten = rewriter.rewrite( sortExpression );
		outer.addSortSpecification(
				rewritten == sortExpression
						? sort
						: new SortSpecification(
								rewritten,
								sort.getSortOrder(),
								sort.getNullPrecedence(),
								sort.isIgnoreCase()
						)
		);
	}

	private static boolean hasSortExpression(List<SortSpecification> sortSpecifications, Expression expression) {
		if ( sortSpecifications != null ) {
			for ( var sortSpecification : sortSpecifications ) {
				if ( expression.equals( sortSpecification.getSortExpression() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static @Nullable TableGroup primaryRoot(List<TableGroup> roots) {
		for ( var root : roots ) {
			if ( hasFetchedPluralReachable( root ) ) {
				return root;
			}
		}
		return null;
	}

	private static boolean hasFetchedPluralReachable(TableGroup group) {
		for ( var join : group.getTableGroupJoins() ) {
			final var joined = join.getJoinedGroup();
			if ( joined.isFetched() ) {
				if ( joined instanceof PluralTableGroup
						|| hasFetchedPluralReachable( joined ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Collect every fetched plural {@code TableGroupJoin} reachable from
	 * {@code group} either directly or through a chain of fetched singulars,
	 * pairing each with its parent {@code TableGroup} so the rewrite can
	 * detach it from the right place.
	 */
	private static void collectMovedPluralFetches(TableGroup group, List<MovedJoin> out) {
		for ( var join : group.getTableGroupJoins() ) {
			final var joined = join.getJoinedGroup();
			if ( joined.isFetched() && !(joined instanceof VirtualTableGroup) ) {
				if ( joined instanceof PluralTableGroup ) {
					out.add( new MovedJoin( group, join ) );
				}
				else {
					// Walk through fetched singulars looking for nested plural fetches.
					collectMovedPluralFetches( joined, out );
				}
			}
		}
	}

	private record MovedJoin(TableGroup parent, TableGroupJoin join) {
	}

	private static boolean sqlAstReferencesAnyAlias(
			SqlAstNode node,
			Set<String> aliases,
			String except) {
		class Walker extends AbstractSqlAstWalker {
			boolean found = false;
			@Override
			public void visitColumnReference(ColumnReference columnReference) {
				final String qualifier = columnReference.getQualifier();
				if ( qualifier != null
						&& !qualifier.equals( except )
						&& aliases.contains( qualifier ) ) {
					found = true;
				}
			}
		}
		final var walker = new Walker();
		node.accept( walker );
		return walker.found;
	}

	private static void collectAliases(TableGroup group, Set<String> aliases) {
		final var primary = group.getPrimaryTableReference();
		if ( primary != null ) {
			aliases.add( primary.getIdentificationVariable() );
		}
		for ( var referenceJoin : group.getTableReferenceJoins() ) {
			aliases.add( referenceJoin.getJoinedTableReference().getIdentificationVariable() );
		}
		for ( var tableGroupJoin : group.getTableGroupJoins() ) {
			collectAliases( tableGroupJoin.getJoinedGroup(), aliases );
		}
		for ( var tableGroupJoin : group.getNestedTableGroupJoins() ) {
			collectAliases( tableGroupJoin.getJoinedGroup(), aliases );
		}
	}

	private record AbsorbedKey(String qualifier, String columnName) {
	}

	private static class AbsorbedColumn {
		final JdbcMapping jdbcMapping;
		String exposedName;
		private AbsorbedColumn(JdbcMapping jdbcMapping) {
			this.jdbcMapping = jdbcMapping;
		}
	}

	/**
	 * Walks expressions looking for {@code ColumnReference}s whose qualifier is
	 * not in the {@code outerAliases} set; each unique (qualifier, column) pair
	 * is recorded for absorption into the derived table.
	 */
	private static class AbsorbedColumnCollector extends AbstractSqlAstWalker {
		private final Set<String> outerAliases;
		private final Map<AbsorbedKey, AbsorbedColumn> absorption;

		private AbsorbedColumnCollector(Set<String> outerAliases, Map<AbsorbedKey, AbsorbedColumn> absorption) {
			this.outerAliases = outerAliases;
			this.absorption = absorption;
		}

		@Override
		public void visitColumnReference(ColumnReference columnReference) {
			final String qualifier = columnReference.getQualifier();
			if ( qualifier != null && !outerAliases.contains( qualifier ) ) {
				absorption.computeIfAbsent(
						new AbsorbedKey( qualifier, columnReference.getColumnExpression() ),
						k -> new AbsorbedColumn( columnReference.getJdbcMapping() )
				);
			}
		}

		@Override
		public void visitSelfRenderingExpression(SelfRenderingExpression expression) {
			// Joined-inheritance synthesizes a CaseStatementDiscriminatorExpression
			// whose inner CaseSearchedExpression is built lazily at SQL render time;
			// trigger the build so the column references it will use are visible to
			// the absorption pass.
			if ( expression instanceof CaseStatementDiscriminatorExpression discriminatorExpression ) {
				discriminatorExpression.buildCaseExpression().accept( this );
			}
		}
	}

	/**
	 * Rewrites {@code ColumnReference}s pointing at an absorbed alias to instead
	 * address the derived table under the prefixed column name.
	 */
	private static class ColumnReferenceRewriter extends ExpressionReplacementWalker {
		private final Map<AbsorbedKey, AbsorbedColumn> absorption;
		private final String primaryAlias;
		// Reuse rewritten ColumnReference instances so repeated visits to the same
		// expression tree keep object identity where possible.
		private final Map<ColumnReference, ColumnReference> cache = new HashMap<>();

		private ColumnReferenceRewriter(Map<AbsorbedKey, AbsorbedColumn> absorption, String primaryAlias) {
			this.absorption = absorption;
			this.primaryAlias = primaryAlias;
		}

		private Expression rewrite(Expression expression) {
			return replaceExpressions( expression );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected <X extends SqlAstNode> X replaceExpression(X expression) {
			if ( expression instanceof ColumnReference columnReference ) {
				final String qualifier = columnReference.getQualifier();
				if ( qualifier != null ) {
					final var absorbedColumn =
							absorption.get( new AbsorbedKey( qualifier,
									columnReference.getColumnExpression() ) );
					if ( absorbedColumn != null ) {
						return (X) cache.computeIfAbsent(
								columnReference,
								original -> new ColumnReference(
										primaryAlias,
										absorbedColumn.exposedName,
										false,
										null,
										original.getJdbcMapping()
								)
						);
					}
				}
			}
			else if ( expression instanceof CaseSearchedExpression caseSearchedExpression ) {
				return (X) rewriteCaseSearched( caseSearchedExpression );
			}
			else if ( expression instanceof CaseSimpleExpression caseSimpleExpression ) {
				return (X) rewriteCaseSimple( caseSimpleExpression );
			}
			else if ( expression instanceof CaseStatementDiscriminatorExpression discriminatorExpression ) {
				// Replace the lazy self-rendering wrapper with the rewritten inner
				// CASE, so the outer renders against the derived alias.
				return (X) rewriteCaseSearched( discriminatorExpression.buildCaseExpression() );
			}
			return expression;
		}

		private CaseSearchedExpression rewriteCaseSearched(CaseSearchedExpression caseSearchedExpression) {
			final var fragments = caseSearchedExpression.getWhenFragments();
			List<CaseSearchedExpression.WhenFragment> newFragments = null;
			for ( int i = 0; i < fragments.size(); i++ ) {
				final var fragment = fragments.get( i );
				final var predicate = fragment.getPredicate();
				final var result = fragment.getResult();
				final var newPred = replaceExpressions( predicate );
				final var newResult = replaceExpressions( result );
				if ( newPred != predicate || newResult != result ) {
					if ( newFragments == null ) {
						newFragments = new ArrayList<>( fragments );
					}
					newFragments.set( i, new CaseSearchedExpression.WhenFragment( newPred, newResult ) );
				}
			}
			final var originalOtherwise = caseSearchedExpression.getOtherwise();
			final var newOtherwise =
					originalOtherwise == null
							? null
							: replaceExpressions( originalOtherwise );
			if ( newFragments != null || newOtherwise != originalOtherwise ) {
				return new CaseSearchedExpression(
						caseSearchedExpression.getExpressionType(),
						newFragments != null ? newFragments : fragments,
						newOtherwise
				);
			}
			return caseSearchedExpression;
		}

		private CaseSimpleExpression rewriteCaseSimple(CaseSimpleExpression caseSimpleExpression) {
			final var newFixture = replaceExpressions( caseSimpleExpression.getFixture() );
			final var fragments = caseSimpleExpression.getWhenFragments();
			List<CaseSimpleExpression.WhenFragment> newFragments = null;
			for ( int i = 0; i < fragments.size(); i++ ) {
				final var fragment = fragments.get( i );
				final var checkValue = fragment.getCheckValue();
				final var result = fragment.getResult();
				final var newCheck = replaceExpressions( checkValue );
				final var newResult = replaceExpressions( result );
				if ( newCheck != checkValue || newResult != result ) {
					if ( newFragments == null ) {
						newFragments = new ArrayList<>( fragments );
					}
					newFragments.set( i, new CaseSimpleExpression.WhenFragment( newCheck, newResult ) );
				}
			}
			final var originalOtherwise = caseSimpleExpression.getOtherwise();
			final var newOtherwise =
					originalOtherwise == null
							? null
							: replaceExpressions( originalOtherwise );
			if ( newFixture != caseSimpleExpression.getFixture()
					|| newFragments != null
					|| newOtherwise != originalOtherwise ) {
				return new CaseSimpleExpression(
						caseSimpleExpression.getExpressionType(),
						newFixture,
						newFragments != null ? newFragments : fragments,
						newOtherwise
				);
			}
			return caseSimpleExpression;
		}
	}
}
