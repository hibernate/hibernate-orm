package org.hibernate.query.sqm.tree.spi.from;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedRootJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Contract representing a from clause.
 * <p>
 * The parent/child bit represents sub-queries.  The child from clauses are only used for test assertions,
 * but are left here as it is most convenient to maintain them here versus another structure.
 *
 * @author Steve Ebersole
 */
public class SqmFromClause implements Serializable, SqmCacheable {
	private static final String SINGLE_ROW_ROOT = "(select 1 as n)";

	private @Nullable List<SqmRoot<?>> domainRoots;

	public SqmFromClause() {
	}

	public SqmFromClause(int expectedNumberOfRoots) {
		domainRoots = arrayList( expectedNumberOfRoots );
	}

	private SqmFromClause(SqmFromClause original, SqmCopyContext context) {
		if ( original.domainRoots != null ) {
			this.domainRoots = new ArrayList<>( original.domainRoots.size() );
			for ( SqmRoot<?> domainRoot : original.domainRoots ) {
				this.domainRoots.add( domainRoot.copy( context ) );
			}
		}
	}

	public SqmFromClause copy(SqmCopyContext context) {
		return new SqmFromClause( this, context );
	}

	/**
	 * Immutable view of the domain roots.  Use {@link #setRoots} or {@link #addRoot} to
	 * mutate the roots
	 */
	public List<SqmRoot<?>> getRoots() {
		return domainRoots == null
				? emptyList()
				: unmodifiableList( domainRoots );
	}

	/**
	 * Inject the complete set of domain roots
	 */
	public void setRoots(List<SqmRoot<?>> domainRoots) {
		this.domainRoots = domainRoots;
	}

	/**
	 * Add roots incrementally
	 */
	public void addRoot(SqmRoot<?> root) {
		if ( domainRoots == null ) {
			domainRoots = new ArrayList<>();
		}
		domainRoots.add( root );
	}

	/**
	 * Visit the domain roots
	 */
	public void visitRoots(Consumer<SqmRoot<?>> consumer) {
		if ( domainRoots != null ) {
			domainRoots.forEach( consumer );
		}
	}

	public int getNumberOfRoots() {
		return domainRoots == null ? 0 : domainRoots.size();
	}

	public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
		appendRoots( sb, context, " " );
	}

	/** Render the FROM clause and collect the inner join predicates that belong in WHERE. */
	public List<SqmPredicate> appendHqlFromClause(StringBuilder sb, SqmRenderContext context) {
		return appendRoots( sb, context, " from " );
	}

	private List<SqmPredicate> appendRoots(StringBuilder sb, SqmRenderContext context, String prefix) {
		boolean hasRoot = false;
		List<SqmPredicate> predicates = null;
		for ( var root : getRoots() ) {
			if ( root.isCorrelated() ) {
				final SqmFrom<?, ?> correlation = root instanceof SqmCorrelatedRootJoin<?> rootJoin
						? rootJoin.getCorrelatedJoin()
						: root;
				if ( correlation.getDeclaredJoins().isEmpty() && !correlation.hasTreats() ) {
					continue;
				}
				if ( !hasRoot ) {
					sb.append( prefix );
				}
				if ( correlation.containsOnlyInnerJoins() ) {
					if ( predicates == null ) {
						predicates = new ArrayList<>();
					}
					appendCorrelatedJoins( correlation, sb, context, hasRoot, predicates );
				}
				else {
					// Outer joins need a left side and must keep their restrictions in ON.
					if ( !hasRoot ) {
						sb.append( SINGLE_ROW_ROOT );
					}
					appendJoins( correlation, sb, context );
					appendTreatJoins( correlation, sb, context );
				}
			}
			else {
				sb.append( hasRoot ? ", " : prefix );
				if ( root instanceof SqmDerivedRoot<?> derivedRoot ) {
					derivedRoot.getQueryPart().appendHqlString( sb, context );
				}
				else if ( root instanceof SqmCteRoot<?> cteRoot ) {
					sb.append( context.resolveCteName( cteRoot.getCte() ) );
				}
				else if ( root instanceof SqmFunctionRoot<?> functionRoot ) {
					functionRoot.getFunction().appendHqlString( sb, context );
				}
				else {
					sb.append( root.getEntityName() );
				}
				sb.append( ' ' ).append( root.resolveAlias( context ) );
				appendJoins( root, sb, context );
				appendTreatJoins( root, sb, context );
			}
			hasRoot = true;
		}
		return predicates == null ? emptyList() : predicates;
	}

	public static void appendJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb, SqmRenderContext context) {
		final List<SqmJoin<?, ?>> orderedJoins;
		if ( sqmFrom instanceof SqmRoot<?> root && ( orderedJoins = root.getOrderedJoins() ) != null ) {
			appendJoins( orderedJoins, sb, false, context );
		}
		else {
			appendJoins( sqmFrom.getDeclaredJoins(), sb, true, context );
		}
	}

	private static void appendJoins(List<? extends SqmJoin<?, ?>> joins, StringBuilder sb, boolean transitive, SqmRenderContext context) {
		for ( var sqmJoin : joins ) {
			appendJoinType( sb, sqmJoin.getSqmJoinType() );
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
				final var sqmTreats = attributeJoin.getSqmTreats();
				if ( attributeJoin.isImplicitJoin() && !sqmTreats.isEmpty() ) {
					for ( int i = 0; i < sqmTreats.size(); i++ ) {
						final var treatJoin = (SqmTreatedAttributeJoin<?, ?, ?>) sqmTreats.get( i );
						if ( i != 0 ) {
							appendJoinType( sb, sqmJoin.getSqmJoinType() );
						}
						if ( treatJoin.isFetched() ) {
							sb.append( "fetch " );
						}
						sb.append( "treat(" );
						appendAttributeJoin( sb, context, attributeJoin );
						sb.append( " as " );
						sb.append( treatJoin.getTreatTarget().getTypeName() );
						sb.append( ')' );
						appendJoinAliasAndOnClause( sb, context, treatJoin );
						if ( transitive ) {
							appendJoins( treatJoin, sb, context );
						}
					}
				}
				else {
					if ( attributeJoin.isFetched() ) {
						sb.append( "fetch " );
					}
					appendAttributeJoin( sb, context, attributeJoin );
					appendJoinAliasAndOnClause( sb, context, attributeJoin );
					if ( transitive ) {
						appendJoins( attributeJoin, sb, context );
						appendTreatJoins( sqmJoin, sb, context );
					}
				}
			}
			else if ( sqmJoin instanceof SqmCrossJoin<?, ?> sqmCrossJoin ) {
				sb.append( sqmCrossJoin.getEntityName() );
				sb.append( ' ' ).append( sqmCrossJoin.resolveAlias( context ) );
				if ( transitive ) {
					appendJoins( sqmJoin, sb, context );
					appendTreatJoins( sqmJoin, sb, context );
				}
			}
			else {
				if ( isLateral( sqmJoin ) ) {
					sb.append( "lateral " );
				}
				appendJoinTarget( sb, context, sqmJoin );
				appendJoinAliasAndOnClause( sb, context, sqmJoin );
				if ( transitive ) {
					appendJoins( sqmJoin, sb, context );
					appendTreatJoins( sqmJoin, sb, context );
				}
			}
		}
	}

	private static boolean isLateral(SqmJoin<?, ?> sqmJoin) {
		return sqmJoin instanceof SqmDerivedJoin<?> derivedJoin && derivedJoin.isLateral()
			|| sqmJoin instanceof SqmFunctionJoin<?> functionJoin && functionJoin.isLateral();
	}

	/**
	 * Renders what is joined by a join which is not an attribute join or a cross join.
	 */
	private static void appendJoinTarget(StringBuilder sb, SqmRenderContext context, SqmJoin<?, ?> sqmJoin) {
		if ( sqmJoin instanceof SqmEntityJoin<?, ?> sqmEntityJoin ) {
			sb.append( sqmEntityJoin.getEntityName() );
		}
		else if ( sqmJoin instanceof SqmDerivedJoin<?> derivedJoin ) {
			derivedJoin.getQueryPart().appendHqlString( sb, context );
		}
		else if ( sqmJoin instanceof SqmCteJoin<?> cteJoin ) {
			sb.append( context.resolveCteName( cteJoin.getCte() ) );
		}
		else if ( sqmJoin instanceof SqmFunctionJoin<?> functionJoin ) {
			functionJoin.getFunction().appendHqlString( sb, context );
		}
		else {
			throw new UnsupportedOperationException( "Unsupported join: " + sqmJoin );
		}
	}

	private static void appendJoinAliasAndOnClause(StringBuilder sb, SqmRenderContext context, SqmJoin<?, ?> join) {
		appendJoinAliasAndOnClause( sb, context, join, null );
	}

	private static void appendJoinAliasAndOnClause(
			StringBuilder sb, SqmRenderContext context, SqmJoin<?, ?> join, @Nullable List<SqmPredicate> wherePredicates) {
		sb.append( ' ' ).append( join.resolveAlias( context ) );
		final var joinPredicate = join.getJoinPredicate();
		if ( joinPredicate != null ) {
			if ( wherePredicates == null ) {
				sb.append( " on " );
				joinPredicate.appendHqlString( sb, context );
			}
			else {
				// A join rendered as a root has no ON clause. Its restriction is equivalent in WHERE.
				wherePredicates.add( joinPredicate );
			}
		}
		if ( join instanceof SqmAttributeJoin<?, ?> attributeJoin && !attributeJoin.isImplicitJoin() ) {
			// A treated variant of an explicit join may have its own predicate and fetch flag.
			for ( var sqmTreat : attributeJoin.getSqmTreats() ) {
				final var treatJoin = (SqmTreatedAttributeJoin<?, ?, ?>) sqmTreat;
				appendJoinType( sb, treatJoin.getSqmJoinType() );
				if ( treatJoin.isFetched() ) {
					sb.append( "fetch " );
				}
				treatJoin.appendHqlString( sb, context );
				appendJoinAliasAndOnClause( sb, context, treatJoin );
			}
		}
	}

	private static void appendAttributeJoin(
			StringBuilder hqlString,
			SqmRenderContext context,
			SqmAttributeJoin<?, ?> attributeJoin) {
		final var owner = attributeJoin.getLhs();
		if ( owner instanceof SqmTreatedPath<?, ?> treatedPath ) {
			hqlString.append( "treat(" );
			treatedPath.getWrappedPath()
					.appendHqlString( hqlString, context );
//					hqlString.append( treatedPath.getWrappedPath().resolveAlias( context ) );
			hqlString.append( " as " )
					.append( treatedPath.getTreatTarget().getTypeName() )
					.append( ')' );
		}
		else {
			hqlString.append( owner.resolveAlias( context ) );
		}
		hqlString.append( '.' )
				.append( attributeJoin.getAttribute().getName() );
	}

	private static void appendJoinType(StringBuilder hqlString, SqmJoinType sqmJoinType) {
//		hqlString.append(' ')
//				.append( sqmJoinType.getText() )
//				.append( " join " );
		hqlString.append( switch ( sqmJoinType ) {
			case LEFT -> " left join ";
			case RIGHT -> " right join ";
			case INNER -> " join ";
			case FULL -> " full join ";
			case CROSS -> " cross join ";
		} );
	}

	private static void appendCorrelatedJoins(
			SqmFrom<?, ?> correlation,
			StringBuilder hql,
			SqmRenderContext context,
			boolean hasRoot,
			List<SqmPredicate> predicates) {
		for ( var join : correlation.getDeclaredJoins() ) {
			if ( isLateral( join ) ) {
				if ( !hasRoot ) {
					// HQL requires a left side for JOIN LATERAL. A single row preserves cardinality.
					hql.append( SINGLE_ROW_ROOT );
				}
				hql.append( " join lateral " );
			}
			else if ( hasRoot ) {
				hql.append( ", " );
			}
			if ( join instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
				hql.append( correlation.getCorrelationParent().resolveAlias( context ) )
						.append( '.' ).append( attributeJoin.getAttribute().getName() );
			}
			else {
				appendJoinTarget( hql, context, join );
			}
			appendJoinAliasAndOnClause( hql, context, join, predicates );
			appendJoins( join, hql, context );
			appendTreatJoins( join, hql, context );
			hasRoot = true;
		}
	}

	public static void appendTreatJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb, SqmRenderContext context) {
		for ( var sqmTreat : sqmFrom.getSqmTreats() ) {
			appendJoins( sqmTreat, sb, context );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmFromClause that
			&& SqmFrom.areDeepEqual( getRoots(), that.getRoots() );
	}

	@Override
	public int hashCode() {
		return getNumberOfRoots();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmFromClause that
			&& SqmFrom.areDeepCompatible( getRoots(), that.getRoots() );
	}

	@Override
	public int cacheHashCode() {
		return getNumberOfRoots();
	}
}
