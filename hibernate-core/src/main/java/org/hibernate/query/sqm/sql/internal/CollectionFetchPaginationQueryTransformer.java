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
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.ExpressionReplacementWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
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

		if ( !( primaryRoot instanceof AbstractTableGroup primaryRootMutable )
				|| !( primaryRoot.getModelPart() instanceof EntityMappingType primaryEntity )
				|| !( primaryRoot.getPrimaryTableReference() instanceof NamedTableReference primaryNamed )
				|| !primaryRoot.getTableReferenceJoins().isEmpty() ) {
			return querySpec;
		}
		final String primaryAlias = primaryNamed.getIdentificationVariable();
		final String primaryTableExpr = primaryNamed.getTableExpression();

		// All fetched joins on the primary root move to the outer query —
		// plural to escape the cartesian-product limit, singular so the columns
		// they project (referenced from the outer) end up reachable.
		final List<TableGroupJoin> movedJoins = new ArrayList<>();
		for ( var join : primaryRoot.getTableGroupJoins() ) {
			if ( join.getJoinedGroup().isFetched() ) {
				movedJoins.add( join );
			}
		}

		// The aliases that will sit directly in the outer FROM after the move.
		final Set<String> outerAliases = new HashSet<>();
		outerAliases.add( primaryAlias );
		for ( var moved : movedJoins ) {
			collectAliases( moved.getJoinedGroup(), outerAliases );
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
		// Moved fetch joins' predicates typically reference only the primary alias
		// (the join target) plus the moved alias itself, so they don't need
		// rewriting. Walk them anyway for the absorbed-alias safety check.
		for ( var moved : movedJoins ) {
			if ( moved.getPredicate() != null ) {
				moved.getPredicate().accept( collector );
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

		// Detach the moved (fetched) joins from the inner primary root.
		for ( var join : movedJoins ) {
			primaryRootMutable.removeTableGroupJoin( join );
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
				converter.getCreationContext().getSessionFactory()
		);
		outer.getFromClause().addRoot( derivedRoot );
		for ( var join : movedJoins ) {
			derivedRoot.addTableGroupJoin( join );
		}

		// Add outer SELECT items, rewriting any absorbed-alias ColumnReferences.
		final var rewriter = new ColumnReferenceRewriter( absorption, primaryAlias );
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

		if ( inner.hasSortSpecifications() ) {
			for ( var sort : inner.getSortSpecifications() ) {
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
		}

		return outer;
	}

	private static @Nullable TableGroup primaryRoot(List<TableGroup> roots) {
		for ( var root : roots ) {
			for ( var join : root.getTableGroupJoins() ) {
				final var joined = join.getJoinedGroup();
				if ( joined instanceof PluralTableGroup && joined.isFetched() ) {
					return root;
				}
			}
		}
		return null;
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
			return expression;
		}
	}
}
