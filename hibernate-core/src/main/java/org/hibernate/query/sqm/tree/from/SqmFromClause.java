/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;

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
	private List<SqmRoot<?>> domainRoots;

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
		return domainRoots == null ? emptyList() : unmodifiableList( domainRoots );
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
		String separator = " ";
		for ( SqmRoot<?> root : getRoots() ) {
			sb.append( separator );
			if ( root.isCorrelated() ) {
				if ( root.containsOnlyInnerJoins() ) {
					appendJoins( root, root.getCorrelationParent().resolveAlias( context ), sb, context );
				}
				else {
					sb.append( root.getCorrelationParent().resolveAlias( context ) );
					sb.append( ' ' ).append( root.resolveAlias( context ) );
					appendJoins( root, sb, context );
					appendTreatJoins( root, sb, context );
				}
			}
			else {
				sb.append( root.getEntityName() );
				sb.append( ' ' ).append( root.resolveAlias( context ) );
				appendJoins( root, sb, context );
				appendTreatJoins( root, sb, context );
			}
			separator = ", ";
		}
	}

	public static void appendJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb, SqmRenderContext context) {
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			switch ( sqmJoin.getSqmJoinType() ) {
				case LEFT:
					sb.append( " left join " );
					break;
				case RIGHT:
					sb.append( " right join " );
					break;
				case INNER:
					sb.append( " join " );
					break;
				case FULL:
					sb.append( " full join " );
					break;
				case CROSS:
					sb.append( " cross join " );
					break;
			}
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
				if ( sqmFrom instanceof SqmTreatedPath<?, ?> treatedPath ) {
					sb.append( "treat(" );
					treatedPath.getWrappedPath().appendHqlString( sb, context );
//					sb.append( treatedPath.getWrappedPath().resolveAlias( context ) );
					sb.append( " as " ).append( treatedPath.getTreatTarget().getTypeName() ).append( ')' );
				}
				else {
					sb.append( sqmFrom.resolveAlias( context ) );
				}
				sb.append( '.' ).append( attributeJoin.getAttribute().getName() );
				sb.append( ' ' ).append( sqmJoin.resolveAlias( context ) );
				if ( attributeJoin.getJoinPredicate() != null ) {
					sb.append( " on " );
					attributeJoin.getJoinPredicate().appendHqlString( sb, context );
				}
				appendJoins( sqmJoin, sb, context );
			}
			else if ( sqmJoin instanceof SqmCrossJoin<?> sqmCrossJoin ) {
				sb.append( sqmCrossJoin.getEntityName() );
				sb.append( ' ' ).append( sqmCrossJoin.resolveAlias( context ) );
				appendJoins( sqmJoin, sb, context );
			}
			else if ( sqmJoin instanceof SqmEntityJoin<?, ?> sqmEntityJoin ) {
				sb.append( sqmEntityJoin.getEntityName() );
				sb.append( ' ' ).append( sqmJoin.resolveAlias( context ) );
				if ( sqmEntityJoin.getJoinPredicate() != null ) {
					sb.append( " on " );
					sqmEntityJoin.getJoinPredicate().appendHqlString( sb, context );
				}
				appendJoins( sqmJoin, sb, context );
			}
			else {
				throw new UnsupportedOperationException( "Unsupported join: " + sqmJoin );
			}
		}
	}

	private void appendJoins(SqmFrom<?, ?> sqmFrom, String correlationPrefix, StringBuilder sb, SqmRenderContext context) {
		String separator = "";
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			assert sqmJoin instanceof SqmAttributeJoin<?, ?>;
			sb.append( separator );
			sb.append( correlationPrefix ).append( '.' );
			sb.append( ( (SqmAttributeJoin<?, ?>) sqmJoin ).getAttribute().getName() );
			sb.append( ' ' ).append( sqmJoin.resolveAlias( context ) );
			appendJoins( sqmJoin, sb, context );
			separator = ", ";
		}
	}

	public static void appendTreatJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb, SqmRenderContext context) {
		for ( SqmFrom<?, ?> sqmTreat : sqmFrom.getSqmTreats() ) {
			appendJoins( sqmTreat, sb, context );
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmFromClause that
			&& this.getNumberOfRoots() == that.getNumberOfRoots()
			// The equals() method for SqmFrom does not do a deep check,
			// so this method must check the full structure now to check compatibility
			&& equalRoots( this.getRoots(), that.getRoots() );
	}

	private boolean equalRoots(List<SqmRoot<?>> theseRoots, List<SqmRoot<?>> thoseRoots) {
		for ( int i = 0; i < theseRoots.size(); i++ ) {
			var thisRoot = theseRoots.get( i );
			var thatRoot = thoseRoots.get( i );
			if ( !thisRoot.equals( thatRoot )
				|| !equalOrderedJoins( thisRoot.getOrderedJoins(), thatRoot.getOrderedJoins() )
				|| !equalJoins( thisRoot.getSqmJoins(), thatRoot.getSqmJoins() ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean equalOrderedJoins(@Nullable List<? extends SqmJoin<?, ?>> theseJoins, @Nullable List<? extends SqmJoin<?, ?>> thoseJoins) {
		if ( theseJoins == null ) {
			return thoseJoins == null;
		}
		else if ( thoseJoins == null || theseJoins.size() != thoseJoins.size() ) {
			return false;
		}
		for ( int i = 0; i < theseJoins.size(); i++ ) {
			var thisJoin = theseJoins.get( i );
			var thatJoin = thoseJoins.get( i );
			if ( !thisJoin.equals( thatJoin )
				|| !joinKindsEqual( thisJoin, thatJoin )
				|| !Objects.equals( thisJoin.getOn(), thatJoin.getOn() ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean equalJoins(List<? extends SqmJoin<?, ?>> theseJoins, List<? extends SqmJoin<?, ?>> thoseJoins) {
		if ( theseJoins.size() != thoseJoins.size() ) {
			return false;
		}
		for ( int i = 0; i < theseJoins.size(); i++ ) {
			var thisJoin = theseJoins.get( i );
			var thatJoin = thoseJoins.get( i );
			if ( !thisJoin.equals( thatJoin )
				|| !joinKindsEqual( thisJoin, thatJoin )
				|| !Objects.equals( thisJoin.getOn(), thatJoin.getOn() )
				|| !equalJoins( thisJoin.getSqmJoins(), thatJoin.getSqmJoins() ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean joinKindsEqual(SqmJoin<?, ?> thisJoin, SqmJoin<?, ?> thatJoin) {
		if ( !Objects.equals( thisJoin.getJoinType(), thatJoin.getJoinType() ) ) {
			return false;
		}
		if ( thisJoin instanceof SqmAttributeJoin<?, ?> thisAttributeJoin
			&& thisAttributeJoin.isFetched() != ((SqmAttributeJoin<?, ?>) thatJoin).isFetched() ) {
			return false;
		}
		if ( thisJoin instanceof SqmFunctionJoin<?> thisFunctionJoin
			&& thisFunctionJoin.isLateral() != ((SqmFunctionJoin<?>) thatJoin).isLateral() ) {
			return false;
		}
		if ( thisJoin instanceof SqmDerivedJoin<?> thisDerivedJoin
			&& thisDerivedJoin.isLateral() != ((SqmDerivedJoin<?>) thatJoin).isLateral() ) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return getNumberOfRoots();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmFromClause that
			&& this.getNumberOfRoots() == that.getNumberOfRoots()
			// The isCompatible() method for SqmFrom does not do a deep check,
			// so this method must check the full structure now to check compatibility
			&& compatibleRoots( this.getRoots(), that.getRoots() );
	}

	private boolean compatibleRoots(List<SqmRoot<?>> theseRoots, List<SqmRoot<?>> thoseRoots) {
		for ( int i = 0; i < theseRoots.size(); i++ ) {
			var thisRoot = theseRoots.get( i );
			var thatRoot = thoseRoots.get( i );
			if ( !thisRoot.isCompatible( thatRoot )
				|| !compatibleOrderedJoins( thisRoot.getOrderedJoins(), thatRoot.getOrderedJoins() )
				|| !compatibleJoins( thisRoot.getSqmJoins(), thatRoot.getSqmJoins() ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean compatibleOrderedJoins(@Nullable List<? extends SqmJoin<?, ?>> theseJoins, @Nullable List<? extends SqmJoin<?, ?>> thoseJoins) {
		if ( theseJoins == null ) {
			return thoseJoins == null;
		}
		else if ( thoseJoins == null || theseJoins.size() != thoseJoins.size() ) {
			return false;
		}
		for ( int i = 0; i < theseJoins.size(); i++ ) {
			var thisJoin = theseJoins.get( i );
			var thatJoin = thoseJoins.get( i );
			if ( !thisJoin.isCompatible( thatJoin )
				|| !joinKindsEqual( thisJoin, thatJoin )
				|| !SqmCacheable.areCompatible( thisJoin.getOn(), thatJoin.getOn() ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean compatibleJoins(List<? extends SqmJoin<?, ?>> theseJoins, List<? extends SqmJoin<?, ?>> thoseJoins) {
		if ( theseJoins.size() != thoseJoins.size() ) {
			return false;
		}
		for ( int i = 0; i < theseJoins.size(); i++ ) {
			var thisJoin = theseJoins.get( i );
			var thatJoin = thoseJoins.get( i );
			if ( !thisJoin.isCompatible( thatJoin )
				|| !joinKindsEqual( thisJoin, thatJoin )
				|| !SqmCacheable.areCompatible( thisJoin.getOn(), thatJoin.getOn() )
				|| !compatibleJoins( thisJoin.getSqmJoins(), thatJoin.getSqmJoins() ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int cacheHashCode() {
		return getNumberOfRoots();
	}
}
