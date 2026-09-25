package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmJunctionPredicate extends AbstractSqmPredicate {
	private final BooleanOperator booleanOperator;
	private final List<SqmPredicate> predicates;

	public SqmJunctionPredicate(
			@Nonnull BooleanOperator booleanOperator,
			@Nullable SqmBindableType<Boolean> expressible,
			@Nonnull NodeBuilder nodeBuilder) {
		super( expressible, nodeBuilder );
		this.booleanOperator = booleanOperator;
		this.predicates = new ArrayList<>();
	}

	public SqmJunctionPredicate(
			@Nonnull BooleanOperator booleanOperator,
			@Nonnull SqmPredicate leftHandPredicate,
			@Nonnull SqmPredicate rightHandPredicate,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder.getBooleanType(), nodeBuilder );
		this.booleanOperator = booleanOperator;
		this.predicates = new ArrayList<>( 2 );
		this.predicates.add( leftHandPredicate );
		this.predicates.add( rightHandPredicate );
	}

	public SqmJunctionPredicate(
			@Nonnull BooleanOperator booleanOperator,
			@Nonnull List<SqmPredicate> predicates,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder.getBooleanType(), nodeBuilder );
		this.booleanOperator = booleanOperator;
		this.predicates = predicates;
	}

	@Nonnull
	@Override
	public SqmJunctionPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmJunctionPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmPredicate> predicates = new ArrayList<>( this.predicates.size() );
		for ( SqmPredicate predicate : this.predicates ) {
			predicates.add( predicate.copy( context ) );
		}
		final SqmJunctionPredicate predicate = context.registerCopy(
				this,
				new SqmJunctionPredicate(
						booleanOperator,
						predicates,
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public List<SqmPredicate> getPredicates() {
		return predicates;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitJunctionPredicate( this );
	}

	@Nonnull
	@Override
	public BooleanOperator getOperator() {
		return booleanOperator;
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Nonnull
	@Override
	public List<Expression<Boolean>> getExpressions() {
		return new ArrayList<>( predicates );
	}

	@Nonnull
	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		final String separator =
				switch ( booleanOperator ) {
					case AND -> " and ";
					case OR -> " or ";
				};
		appendJunctionHqlString( predicates.get( 0 ), hql, context );
		for ( int i = 1; i < predicates.size(); i++ ) {
			hql.append( separator );
			appendJunctionHqlString( predicates.get( i ), hql, context );
		}
	}

	private void appendJunctionHqlString(@Nonnull SqmPredicate p, @Nonnull StringBuilder sb, @Nonnull SqmRenderContext context) {
		if ( p instanceof SqmJunctionPredicate junction ) {
			// If we have the same nature, or if this is a disjunction and the operand is a conjunction,
			// then we don't need parenthesis, because the AND operator binds stronger
			if ( booleanOperator == junction.getOperator() || booleanOperator == BooleanOperator.OR ) {
				junction.appendHqlString( sb, context );
			}
			else {
				sb.append( '(' );
				junction.appendHqlString( sb, context );
				sb.append( ')' );
			}
		}
		else {
			p.appendHqlString( sb, context );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmJunctionPredicate that
			&& booleanOperator == that.booleanOperator
			&& Objects.equals( predicates, that.predicates );
	}

	@Override
	public int hashCode() {
		int result = booleanOperator.hashCode();
		result = 31 * result + Objects.hashCode( predicates );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmJunctionPredicate that
			&& booleanOperator == that.booleanOperator
			&& SqmCacheable.areCompatible( predicates, that.predicates );
	}

	@Override
	public int cacheHashCode() {
		int result = booleanOperator.hashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( predicates );
		return result;
	}
}
