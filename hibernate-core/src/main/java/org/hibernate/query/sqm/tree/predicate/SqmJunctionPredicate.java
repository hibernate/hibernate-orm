/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmJunctionPredicate extends AbstractSqmPredicate {
	private final BooleanOperator booleanOperator;
	private final List<SqmPredicate> predicates;

	public SqmJunctionPredicate(
			BooleanOperator booleanOperator,
			SqmExpressible<Boolean> expressible,
			NodeBuilder nodeBuilder) {
		super( expressible, nodeBuilder );
		this.booleanOperator = booleanOperator;
		this.predicates = new ArrayList<>();
	}

	public SqmJunctionPredicate(
			BooleanOperator booleanOperator,
			SqmPredicate leftHandPredicate,
			SqmPredicate rightHandPredicate,
			NodeBuilder nodeBuilder) {
		super( leftHandPredicate.getExpressible(), nodeBuilder );
		this.booleanOperator = booleanOperator;
		this.predicates = new ArrayList<>( 2 );
		this.predicates.add( leftHandPredicate );
		this.predicates.add( rightHandPredicate );
	}

	public SqmJunctionPredicate(
			BooleanOperator booleanOperator,
			List<SqmPredicate> predicates,
			NodeBuilder nodeBuilder) {
		super( predicates.get( 0 ).getNodeType(), nodeBuilder );
		this.booleanOperator = booleanOperator;
		this.predicates = predicates;
	}

	@Override
	public SqmJunctionPredicate copy(SqmCopyContext context) {
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

	public List<SqmPredicate> getPredicates() {
		return predicates;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitJunctionPredicate( this );
	}

	@Override
	public BooleanOperator getOperator() {
		return booleanOperator;
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return new ArrayList<>( predicates );
	}

	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
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

	private void appendJunctionHqlString(SqmPredicate p, StringBuilder sb, SqmRenderContext context) {
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
	public boolean equals(Object object) {
		if ( !(object instanceof SqmJunctionPredicate that) ) {
			return false;
		}
		return booleanOperator == that.booleanOperator
			&& Objects.equals( predicates, that.predicates );
	}

	@Override
	public int hashCode() {
		return Objects.hash( booleanOperator, predicates );
	}
}
