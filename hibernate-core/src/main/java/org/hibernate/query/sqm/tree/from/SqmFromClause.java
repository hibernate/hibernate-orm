/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;

/**
 * Contract representing a from clause.
 * <p>
 * The parent/child bit represents sub-queries.  The child from clauses are only used for test assertions,
 * but are left here as it is most convenient to maintain them here versus another structure.
 *
 * @author Steve Ebersole
 */
public class SqmFromClause implements Serializable {
	private List<SqmRoot<?>> domainRoots;

	public SqmFromClause() {
	}

	public SqmFromClause(int expectedNumberOfRoots) {
		this.domainRoots = CollectionHelper.arrayList( expectedNumberOfRoots );
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
		return domainRoots == null ? Collections.emptyList() : Collections.unmodifiableList( domainRoots );
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
		if ( domainRoots == null ) {
			return 0;
		}
		else {
			return domainRoots.size();
		}
	}

	public void appendHqlString(StringBuilder sb) {
		String separator = " ";
		for ( SqmRoot<?> root : getRoots() ) {
			sb.append( separator );
			if ( root.isCorrelated() ) {
				if ( root.containsOnlyInnerJoins() ) {
					appendJoins( root, root.getCorrelationParent().resolveAlias(), sb );
				}
				else {
					sb.append( root.getCorrelationParent().resolveAlias() );
					sb.append( ' ' ).append( root.resolveAlias() );
					appendJoins( root, sb );
					appendTreatJoins( root, sb );
				}
			}
			else {
				sb.append( root.getEntityName() );
				sb.append( ' ' ).append( root.resolveAlias() );
				appendJoins( root, sb );
				appendTreatJoins( root, sb );
			}
			separator = ", ";
		}
	}

	public static void appendJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb) {
		if ( sqmFrom instanceof SqmRoot<?> && ( (SqmRoot<?>) sqmFrom ).getOrderedJoins() != null ) {
			appendJoins( sqmFrom, ( (SqmRoot<?>) sqmFrom ).getOrderedJoins(), sb, false );
		}
		else {
			appendJoins( sqmFrom, sqmFrom.getSqmJoins(), sb, true );
		}
	}

	private static void appendJoins(SqmFrom<?, ?> sqmFrom, List<? extends SqmJoin<?, ?>> joins, StringBuilder sb, boolean transitive) {
		for ( SqmJoin<?, ?> sqmJoin : joins ) {
			appendJoinType( sb, sqmJoin.getSqmJoinType() );
			if ( sqmJoin instanceof SqmAttributeJoin<?, ?> ) {
				final SqmAttributeJoin<?, ?> attributeJoin = (SqmAttributeJoin<?, ?>) sqmJoin;
				final List<SqmFrom<?, ?>> sqmTreats = attributeJoin.getSqmTreats();
				if ( attributeJoin.getExplicitAlias() != null && !sqmTreats.isEmpty() ) {
					for ( int i = 0; i < sqmTreats.size(); i++ ) {
						final var treatJoin = (SqmAttributeJoin<?, ?>) sqmTreats.get( i );
						if ( i != 0 ) {
							appendJoinType( sb, sqmJoin.getSqmJoinType() );
						}
						sb.append( "treat(" );
						appendAttributeJoin( sqmFrom, sb, attributeJoin );
						sb.append( " as " );
						sb.append( ((SqmTreatedPath<?, ?>) treatJoin).getTreatTarget().getTypeName() );
						sb.append( ')' );
						appendJoinAliasAndOnClause( sb, treatJoin );
						if ( transitive ) {
							appendJoins( treatJoin, sb );
						}
					}
				}
				else {
					appendAttributeJoin( sqmFrom, sb, attributeJoin );
					appendJoinAliasAndOnClause( sb, attributeJoin );
					if ( transitive ) {
						appendJoins( attributeJoin, sb );
						appendTreatJoins( sqmJoin, sb );
					}
				}
			}
			else if ( sqmJoin instanceof SqmCrossJoin<?> ) {
				sb.append( ( (SqmCrossJoin<?>) sqmJoin ).getEntityName() );
				sb.append( ' ' ).append( sqmJoin.resolveAlias() );
				if ( transitive ) {
					appendJoins( sqmJoin, sb );
					appendTreatJoins( sqmJoin, sb );
				}
			}
			else if ( sqmJoin instanceof SqmEntityJoin<?> ) {
				final SqmEntityJoin<?> sqmEntityJoin = (SqmEntityJoin<?>) sqmJoin;
				sb.append( sqmEntityJoin.getEntityName() );
				appendJoinAliasAndOnClause( sb, sqmEntityJoin );
				if ( transitive ) {
					appendJoins( sqmJoin, sb );
					appendTreatJoins( sqmJoin, sb );
				}
			}
			else {
				throw new UnsupportedOperationException( "Unsupported join: " + sqmJoin );
			}
		}
	}

	private static void appendJoinAliasAndOnClause(StringBuilder sb, SqmQualifiedJoin<?, ?> join) {
		sb.append( ' ' ).append( join.resolveAlias() );
		if ( join.getJoinPredicate() != null ) {
			sb.append( " on " );
			join.getJoinPredicate().appendHqlString( sb );
		}
	}

	private static void appendAttributeJoin(SqmFrom<?, ?> sqmFrom, StringBuilder sb, SqmAttributeJoin<?, ?> attributeJoin) {
		if ( sqmFrom instanceof SqmTreatedPath<?, ?> ) {
			final SqmTreatedPath<?, ?> treatedPath = (SqmTreatedPath<?, ?>) sqmFrom;
			sb.append( "treat(" );
			treatedPath.getWrappedPath().appendHqlString( sb );
//					sb.append( treatedPath.getWrappedPath().resolveAlias( context ) );
			sb.append( " as " ).append( treatedPath.getTreatTarget().getTypeName() ).append( ')' );
		}
		else {
			sb.append( sqmFrom.resolveAlias() );
		}
		sb.append( '.' ).append( attributeJoin.getAttribute().getName() );
	}

	private static void appendJoinType(StringBuilder sb, SqmJoinType sqmJoinType) {
		final String joinText;
		switch ( sqmJoinType ) {
			case LEFT:
				joinText = " left join ";
				break;
			case RIGHT:
				joinText = " right join ";
				break;
			case INNER:
				joinText = " join ";
				break;
			case FULL:
				joinText = " full join ";
				break;
			case CROSS:
				joinText = " cross join ";
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported join type: " + sqmJoinType );
		}
		sb.append( joinText );
	}

	private void appendJoins(SqmFrom<?, ?> sqmFrom, String correlationPrefix, StringBuilder sb) {
		String separator = "";
		for ( SqmJoin<?, ?> sqmJoin : sqmFrom.getSqmJoins() ) {
			assert sqmJoin instanceof SqmAttributeJoin<?, ?>;
			sb.append( separator );
			sb.append( correlationPrefix ).append( '.' );
			sb.append( ( (SqmAttributeJoin<?, ?>) sqmJoin ).getAttribute().getName() );
			sb.append( ' ' ).append( sqmJoin.resolveAlias() );
			appendJoins( sqmJoin, sb );
			separator = ", ";
		}
	}

	public static void appendTreatJoins(SqmFrom<?, ?> sqmFrom, StringBuilder sb) {
		for ( SqmFrom<?, ?> sqmTreat : sqmFrom.getSqmTreats() ) {
			appendJoins( sqmTreat, sb );
		}
	}
}
