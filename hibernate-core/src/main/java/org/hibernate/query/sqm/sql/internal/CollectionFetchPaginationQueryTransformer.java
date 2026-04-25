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
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl;
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
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.results.internal.ResolvedSqlSelection;
import org.hibernate.type.BasicType;

/**
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
 */
public class CollectionFetchPaginationQueryTransformer implements QueryTransformer {

	@Override
	public QuerySpec transform(
			CteContainer cteContainer,
			QuerySpec querySpec,
			SqmToSqlAstConverter converter) {

		final List<TableGroup> roots = querySpec.getFromClause().getRoots();
		if ( roots.isEmpty() ) {
			return querySpec;
		}

		// Pick the first root that has at least one plural fetched join — that's the
		// "primary" root whose alias becomes the derived-table identification variable.
		final var primaryRoot = primaryRoot( roots );
		if ( primaryRoot == null ) {
			return querySpec;
		}

		if ( !( primaryRoot instanceof AbstractTableGroup )
				|| !( primaryRoot.getModelPart() instanceof EntityMappingType primaryEntity )
				|| !( primaryRoot.getPrimaryTableReference() instanceof NamedTableReference primaryNamed ) ) {
			return querySpec;
		}
		final String primaryAlias = primaryNamed.getIdentificationVariable();
		final String primaryTableExpr = primaryNamed.getTableExpression();

		// Only plural fetched joins move to the outer — they are the
		// cartesian-product source we're trying to escape. Fetched singular
		// joins stay inside the inner so their columns can drive the
		// pagination's ORDER BY (e.g. {@code order by p.name} where {@code p}
		// is a fetched {@code @ManyToOne}); their columns get absorbed into
		// the derived table for the outer SELECT to reach. Plural fetches
		// nested under fetched singulars are also moved out — the parent
		// singular's table reference stays inner and the plural's join
		// predicate gets absorbed-rewritten through the derived alias.
		final List<MovedJoin> movedJoins = new ArrayList<>();
		collectMovedPluralFetches( primaryRoot, movedJoins );

		// The aliases that will sit directly in the outer FROM after the move.
		final Set<String> outerAliases = new HashSet<>();
		outerAliases.add( primaryAlias );
		for ( var moved : movedJoins ) {
			collectAliases( moved.join().getJoinedGroup(), outerAliases );
		}


		// Walk the original outer content for any ColumnReference whose qualifier
		// is neither the primary alias nor inside a moved-out join. Those qualifiers
		// belong to TableGroups that stay in the inner — they need to be "absorbed"
		// into the derived table.
		final Map<AbsorbedKey, AbsorbedColumn> absorption = new LinkedHashMap<>();
		final var collector = new AbsorbedColumnCollector( outerAliases, absorption );
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
		// the primary alias and no rewriting is needed; for nested moves the
		// parent is a fetched singular's alias which is inner-only and gets
		// absorbed into the derived table here.
		for ( var moved : movedJoins ) {
			final var predicate = moved.join().getPredicate();
			if ( predicate != null ) {
				predicate.accept( collector );
			}
		}

		// Sanity: every absorbed qualifier must correspond to a TableGroup in the inner.
		final Set<String> innerAliases = new HashSet<>();
		for ( var root : roots ) {
			collectAliases( root, innerAliases );
		}
		// (the primary alias is in both — that's fine)
		for ( var key : absorption.keySet() ) {
			if ( !innerAliases.contains( key.qualifier() ) ) {
				return querySpec;
			}
		}

		// Inner-join fetches act as a filter on the parent rows (only parents that
		// have at least one matching child appear). To keep that semantics when the
		// fetch join moves to the outer, add an EXISTS predicate to the inner
		// referencing the joined group, so the inner pagination only sees parents
		// that would have matched the inner join.
		final var sessionFactory = converter.getCreationContext().getSessionFactory();
		for ( var moved : movedJoins ) {
			final var join = moved.join();
			if ( join.getJoinType() == org.hibernate.sql.ast.SqlAstJoinType.INNER
					&& join.getPredicate() != null ) {
				final var existsSpec = new QuerySpec( false, 1 );
				existsSpec.getFromClause().addRoot( join.getJoinedGroup() );
				existsSpec.getSelectClause().addSqlSelection(
						new ResolvedSqlSelection(
								0,
								new org.hibernate.sql.ast.tree.expression.QueryLiteral<>(
										1,
										sessionFactory.getTypeConfiguration()
												.getBasicTypeForJavaType( Integer.class )
								),
								(BasicType<Object>) (BasicType<?>)
										sessionFactory.getTypeConfiguration()
												.getBasicTypeForJavaType( Integer.class )
						)
				);
				existsSpec.applyPredicate( join.getPredicate() );
				querySpec.applyPredicate(
						new ExistsPredicate(
								new SelectStatement( existsSpec ),
								false,
								sessionFactory.getTypeConfiguration()
										.getBasicTypeForJavaType( Boolean.class )
						)
				);
			}
		}

		// Capture original outer SELECT items before we rebuild the inner SELECT.
		final List<SqlSelection> originalSelections =
				new ArrayList<>( querySpec.getSelectClause().getSqlSelections() );

		// Rebuild inner SELECT: primary entity's primary-table columns (kept under their
		// original names so outer refs to the primary alias resolve naturally) +
		// absorbed columns (renamed to <alias>_<col> in the derived column list).
		final List<String> columnNames = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		querySpec.getSelectClause().getSqlSelections().clear();
		final int[] position = { 0 };
		final SelectableConsumer projector = (idx, selectable) -> {
			if ( primaryTableExpr.equals( selectable.getContainingTableExpression() ) ) {
				final String columnName = selectable.getSelectionExpression();
				// Joined-inheritance with no @DiscriminatorColumn synthesises a CASE
				// expression on the subtype tables and exposes it through a placeholder
				// selectable like "{discriminator}". The CASE itself is in the outer
				// SELECT and AbsorbedColumnCollector picks up its component column refs,
				// so the inner doesn't need (and can't render) this placeholder.
				if ( columnName.indexOf( '{' ) >= 0 ) {
					return;
				}
				if ( seen.add( columnName ) ) {
					final var ref = new ColumnReference( primaryAlias, selectable );
					//noinspection unchecked
					final var type = (BasicType<Object>) selectable.getJdbcMapping();
					querySpec.getSelectClause().addSqlSelection(
							new ResolvedSqlSelection( position[0]++, ref, type )
					);
					columnNames.add( columnName );
				}
			}
		};
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

		for ( var entry : absorption.entrySet() ) {
			final var key = entry.getKey();
			final var info = entry.getValue();
			final String exposedName = key.qualifier() + "_" + key.columnName();
			final var ref = new ColumnReference(
					key.qualifier(),
					key.columnName(),
					false,
					null,
					info.jdbcMapping
			);
			//noinspection unchecked
			final var type = (BasicType<Object>) info.jdbcMapping;
			querySpec.getSelectClause().addSqlSelection(
					new ResolvedSqlSelection( position[0]++, ref, type )
			);
			columnNames.add( exposedName );
			info.exposedName = exposedName;
		}

		// Detach the moved (fetched) plural joins from whichever group they were
		// attached to in the inner — the primary root for direct fetches, or a
		// fetched-singular's group for nested fetches.
		for ( var moved : movedJoins ) {
			moved.parent().removeTableGroupJoin( moved.join() );
		}

		// Split sort specs: those that reference an alias that's now in the outer
		// (e.g. the synthetic ORDER BY Hibernate adds on a fetched collection's FK)
		// can't stay on the inner — they'd reference a TableGroup we just removed.
		// We keep all original sort specs to apply on the outer; only the
		// inner-resolvable ones stay in the inner for deterministic LIMIT.
		final List<SortSpecification> originalSortSpecs;
		if ( querySpec.hasSortSpecifications() ) {
			originalSortSpecs = new ArrayList<>( querySpec.getSortSpecifications() );
			final var innerKeep = new ArrayList<SortSpecification>( originalSortSpecs.size() );
			for ( var sort : originalSortSpecs ) {
				if ( !expressionReferencesAnyAlias( sort.getSortExpression(), outerAliases, primaryAlias ) ) {
					innerKeep.add( sort );
				}
			}
			querySpec.getSortSpecifications().clear();
			querySpec.getSortSpecifications().addAll( innerKeep );
		}
		else {
			originalSortSpecs = List.of();
		}

		// Demote the original spec to a sub-query (offset/fetch/order-by stay on it).
		final var inner = querySpec.asSubQuery();

		// Build the outer query.
		final var outer = new QuerySpec( querySpec.isRoot(), 1 );
		final var derivedRoot = new QueryPartTableGroup(
				primaryRoot.getNavigablePath(),
				null,
				new SelectStatement( inner ),
				primaryAlias,
				columnNames,
				new HashSet<>( Set.of( primaryTableExpr ) ),
				false,
				true,
				sessionFactory
		);
		outer.getFromClause().addRoot( derivedRoot );

		// Add outer SELECT items and reattach moved fetch joins, rewriting any
		// absorbed-alias ColumnReferences.
		final var rewriter = new ColumnReferenceRewriter( absorption, primaryAlias );
		for ( var moved : movedJoins ) {
			final var join = moved.join();
			final Predicate originalPredicate = join.getPredicate();
			if ( originalPredicate == null ) {
				derivedRoot.addTableGroupJoin( join );
			}
			else {
				final Predicate rewrittenPredicate = rewriter.replaceExpressions( originalPredicate );
				if ( rewrittenPredicate == originalPredicate ) {
					derivedRoot.addTableGroupJoin( join );
				}
				else {
					// Predicates carry a TableGroup reference at construction time —
					// rebuild the join with the rewritten predicate so the outer's
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
		for ( var sel : originalSelections ) {
			final Expression rewritten = rewriter.rewrite( sel.getExpression() );
			if ( rewritten == sel.getExpression() ) {
				outer.getSelectClause().addSqlSelection( sel );
			}
			else {
				//noinspection unchecked
				final var type = (BasicType<Object>) rewritten.getExpressionType().getSingleJdbcMapping();
				outer.getSelectClause().addSqlSelection(
						new ResolvedSqlSelection( sel.getValuesArrayPosition(), rewritten, type )
				);
			}
		}

		// Outer ORDER BY mirrors the original (full) sort specs, with absorbed
		// column references rewritten through the derived alias.
		for ( var sort : originalSortSpecs ) {
			final var rewritten = rewriter.rewrite( sort.getSortExpression() );
			outer.addSortSpecification(
					rewritten == sort.getSortExpression()
							? sort
							: new SortSpecification(
									rewritten,
									sort.getSortOrder(),
									sort.getNullPrecedence()
							)
			);
		}

		return outer;
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
			if ( joined.isFetched()
					&& !(joined instanceof VirtualTableGroup) ) {
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

	private static boolean expressionReferencesAnyAlias(
			Expression expression,
			Set<String> aliases,
			String except) {
		final boolean[] found = { false };
		expression.accept( new AbstractSqlAstWalker() {
			@Override
			public void visitColumnReference(ColumnReference columnReference) {
				final String qualifier = columnReference.getQualifier();
				if ( qualifier != null
						&& !qualifier.equals( except )
						&& aliases.contains( qualifier ) ) {
					found[0] = true;
				}
			}
		} );
		return found[0];
	}

	private static void collectAliases(TableGroup group, Set<String> aliases) {
		final var primary = group.getPrimaryTableReference();
		if ( primary != null ) {
			aliases.add( primary.getIdentificationVariable() );
		}
		for ( var refJoin : group.getTableReferenceJoins() ) {
			aliases.add( refJoin.getJoinedTableReference().getIdentificationVariable() );
		}
		for ( var tgj : group.getTableGroupJoins() ) {
			collectAliases( tgj.getJoinedGroup(), aliases );
		}
		for ( var tgj : group.getNestedTableGroupJoins() ) {
			collectAliases( tgj.getJoinedGroup(), aliases );
		}
	}

	private record AbsorbedKey(String qualifier, String columnName) {
	}

	private static class AbsorbedColumn {
		final JdbcMapping jdbcMapping;
		String exposedName;

		AbsorbedColumn(JdbcMapping jdbcMapping) {
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
		public void visitSelfRenderingExpression(
				org.hibernate.sql.ast.tree.expression.SelfRenderingExpression expression) {
			// Joined-inheritance synthesises a CaseStatementDiscriminatorExpression
			// whose inner CaseSearchedExpression is built lazily at SQL render time;
			// trigger the build so the column references it will use are visible to
			// the absorption pass.
			if ( expression instanceof CaseStatementDiscriminatorMappingImpl.CaseStatementDiscriminatorExpression cdsExpr ) {
				cdsExpr.buildCaseExpression().accept( this );
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
		// Cache replacements for identity preservation across repeated visits.
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
					final var info =
							absorption.get( new AbsorbedKey( qualifier,
									columnReference.getColumnExpression() ) );
					if ( info != null ) {
						return (X) cache.computeIfAbsent(
								columnReference,
								original -> new ColumnReference(
										primaryAlias,
										info.exposedName,
										false,
										null,
										original.getJdbcMapping()
								)
						);
					}
				}
			}
			else if ( expression instanceof CaseSearchedExpression cse ) {
				return (X) rewriteCaseSearched( cse );
			}
			else if ( expression instanceof CaseSimpleExpression cse ) {
				return (X) rewriteCaseSimple( cse );
			}
			else if ( expression instanceof CaseStatementDiscriminatorMappingImpl.CaseStatementDiscriminatorExpression cdsExpr ) {
				// Replace the lazy self-rendering wrapper with the rewritten inner
				// CASE, so the outer renders against the derived alias.
				return (X) rewriteCaseSearched( cdsExpr.buildCaseExpression() );
			}
			return expression;
		}

		private CaseSearchedExpression rewriteCaseSearched(CaseSearchedExpression cse) {
			final var fragments = cse.getWhenFragments();
			List<CaseSearchedExpression.WhenFragment> newFragments = null;
			for ( int i = 0; i < fragments.size(); i++ ) {
				final var fragment = fragments.get( i );
				final Predicate newPred = replaceExpressions( fragment.getPredicate() );
				final Expression newResult = replaceExpressions( fragment.getResult() );
				if ( newPred != fragment.getPredicate() || newResult != fragment.getResult() ) {
					if ( newFragments == null ) {
						newFragments = new ArrayList<>( fragments );
					}
					newFragments.set( i, new CaseSearchedExpression.WhenFragment( newPred, newResult ) );
				}
			}
			final Expression originalOtherwise = cse.getOtherwise();
			final Expression newOtherwise = originalOtherwise == null
					? null
					: replaceExpressions( originalOtherwise );
			if ( newFragments != null || newOtherwise != originalOtherwise ) {
				return new CaseSearchedExpression(
						cse.getExpressionType(),
						newFragments != null ? newFragments : fragments,
						newOtherwise
				);
			}
			return cse;
		}

		private CaseSimpleExpression rewriteCaseSimple(CaseSimpleExpression cse) {
			final Expression newFixture = replaceExpressions( cse.getFixture() );
			final var fragments = cse.getWhenFragments();
			List<CaseSimpleExpression.WhenFragment> newFragments = null;
			for ( int i = 0; i < fragments.size(); i++ ) {
				final var fragment = fragments.get( i );
				final Expression newCheck = replaceExpressions( fragment.getCheckValue() );
				final Expression newResult = replaceExpressions( fragment.getResult() );
				if ( newCheck != fragment.getCheckValue() || newResult != fragment.getResult() ) {
					if ( newFragments == null ) {
						newFragments = new ArrayList<>( fragments );
					}
					newFragments.set( i, new CaseSimpleExpression.WhenFragment( newCheck, newResult ) );
				}
			}
			final Expression originalOtherwise = cse.getOtherwise();
			final Expression newOtherwise = originalOtherwise == null
					? null
					: replaceExpressions( originalOtherwise );
			if ( newFixture != cse.getFixture() || newFragments != null
					|| newOtherwise != originalOtherwise ) {
				return new CaseSimpleExpression(
						cse.getExpressionType(),
						newFixture,
						newFragments != null ? newFragments : fragments,
						newOtherwise
				);
			}
			return cse;
		}
	}
}
