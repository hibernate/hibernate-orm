package org.hibernate.dialect.lock.internal;

import java.util.Collection;

import jakarta.persistence.PessimisticLockScope;

import org.hibernate.Internal;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.analysis.AggregateFunctionChecker;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.results.graph.internal.LockingResultCoverage;

/// Conservative eligibility for direct locking of a single preserved entity
/// root. Targeting support and join eligibility are independent requirements.
/// This checker neither changes the SQL AST nor initializes lazy table groups.
///
/// @author Steve Ebersole
@Internal
public final class NonNullLockTargetChecker {
	private NonNullLockTargetChecker() {
	}

	/// Determines whether a selection satisfies the conservative eligibility rules
	/// for [org.hibernate.dialect.lock.spi.OuterJoinLockingType#NON_NULL_TARGETS].
	/// The caller must separately establish that the dialect advertises this capability.
	///
	/// Eligibility requires a top-level, unpaginated query selecting a complete,
	/// single-table entity root as its sole lock target under [PessimisticLockScope#NORMAL].
	/// Only transparent INNER/LEFT to-one join trees are accepted, so joined rows
	/// may be absent while the root itself is never null-extended. Result transformations
	/// such as DISTINCT, grouping and aggregation remain outside this optimization.
	///
	/// A `false` result means this checker cannot establish eligibility; the caller
	/// must apply the query's follow-on locking policy. No AST or result-coverage
	/// state is changed, and lazy table groups are never initialized by this check.
	///
	/// @param querySpec the query specification whose lock clause is being considered
	/// @param statement the complete selection containing the query and domain results
	/// @param registeredTargets the actual paths registered by the locking-clause strategy;
	///        null or empty collections cannot establish an explicitly targeted lock
	/// @param scope the requested pessimistic lock scope
	/// @param targeting the row-lock targeting mechanism for the requested lock mode;
	///        null and [RowLockStrategy#NONE] cannot express the required exact target
	/// @param paginated whether query options impose pagination, in addition to any
	///        offset or fetch clause present in the AST
	///
	/// @return `true` when direct locking can be limited to the preserved entity root
	public static boolean canApply(
			QuerySpec querySpec,
			SelectStatement statement,
			Collection<NavigablePath> registeredTargets,
			PessimisticLockScope scope,
			RowLockStrategy targeting,
			boolean paginated) {
		if ( scope != PessimisticLockScope.NORMAL || targeting == null || targeting == RowLockStrategy.NONE
				|| !querySpec.isRoot() || statement.getQueryPart() != querySpec
				|| paginated || querySpec.hasOffsetOrFetchClause()
				|| querySpec.getSelectClause().isDistinct()
				|| !querySpec.getGroupByClauseExpressions().isEmpty()
				|| querySpec.getHavingClauseRestrictions() != null
				|| AggregateFunctionChecker.hasAggregateFunctions( querySpec )
				|| querySpec.getFromClause().getRoots().size() != 1 ) {
			return false;
		}

		final var root = querySpec.getFromClause().getRoots().get( 0 );
		// The SQL alias identifies the table to lock; the source alias lets result
		// processing apply the acquired mode to the root initializer alone.
		if ( !(root instanceof StandardTableGroup)
				|| !(root.getModelPart() instanceof EntityMappingType entity)
				|| entity.getEntityPersister().getSubclassTableSpan() != 1
				|| root.getSourceAlias() == null || !isPhysicalTable( root ) ) {
			return false;
		}
		final var path = root.getNavigablePath();
		// Both the query's required targets and the strategy's registered targets
		// must agree. A nullable or additional target cannot be silently dropped.
		if ( path == null || !isSoleTarget( path, querySpec.getRootPathsForLocking() )
				|| !isSoleTarget( path, registeredTargets ) ) {
			return false;
		}

		var coverage = querySpec.getLockingResultCoverage();
		if ( coverage == null ) {
			// Manually constructed selections may lack the converter's coverage
			// metadata. Derive it locally without attaching new state to the AST.
			coverage = LockingResultCoverage.EMPTY;
			for ( var result : statement.getDomainResultDescriptors() ) {
				coverage = coverage.withDomainResult( result );
			}
		}
		return coverage.entityPaths().contains( path ) && hasOnlyToOneJoins( root, root.getSourceAlias() );
	}

	private static boolean isSoleTarget(NavigablePath path, Collection<NavigablePath> targets) {
		return targets != null && targets.size() == 1 && targets.contains( path );
	}

	private static boolean isPhysicalTable(TableGroup group) {
		if ( !group.isInitialized() || group.isVirtual() || group.isLateral()
				|| !group.getTableReferenceJoins().isEmpty() ) {
			return false;
		}
		final var reference = group.getPrimaryTableReference();
		if ( reference == null || reference.getClass() != NamedTableReference.class ) {
			// Excludes union, derived, CTE and otherwise opaque table references.
			return false;
		}
		final var named = (NamedTableReference) reference;
		// Mapped subselects also use NamedTableReference, while temporal references
		// may render a derived source instead of the current physical table.
		return !named.getTableExpression().isBlank()
				&& !named.getTableExpression().stripLeading().startsWith( "(" )
				&& named.getAsOfTransactionIdentifier() == null
				&& named.getIdentificationVariable() != null
				&& !named.getIdentificationVariable().isBlank();
	}

	private static boolean hasOnlyToOneJoins(TableGroup group, String rootAlias) {
		for ( var join : group.getTableGroupJoins() ) {
			if ( !isEligibleJoin( join, rootAlias ) ) {
				return false;
			}
		}
		// Nested joins can hide unsupported descendants just as ordinary joins can.
		for ( var join : group.getNestedTableGroupJoins() ) {
			if ( !isEligibleJoin( join, rootAlias ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEligibleJoin(TableGroupJoin join, String rootAlias) {
		final var group = join.getJoinedGroup();
		if ( !group.isInitialized() ) {
			// An unused lazy association renders no join. Do not initialize it to
			// inspect the table, but reject any real descendants we cannot verify.
			return !group.hasRealJoins();
		}
		if ( group instanceof StandardVirtualTableGroup && group.getModelPart() instanceof EmbeddableValuedModelPart ) {
			// Identifier and embedded component groups introduce no SQL join;
			// their descendants still need the full join-eligibility check.
			return hasOnlyToOneJoins( group, rootAlias );
		}
		// Keep the preservation proof bounded to INNER/LEFT to-one trees. A
		// distinct source alias also prevents a fetched initializer matching the root.
		return (join.getJoinType() == SqlAstJoinType.LEFT || join.getJoinType() == SqlAstJoinType.INNER)
				&& group.getModelPart() instanceof ToOneAttributeMapping toOne
				&& toOne.getAssociatedEntityMappingType().getEntityPersister().getSubclassTableSpan() == 1
				&& !rootAlias.equals( group.getSourceAlias() )
				&& isPhysicalTable( group ) && hasOnlyToOneJoins( group, rootAlias );
	}
}
