/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;

/**
 * A tuple constructor, that is, a list of expressions wrapped in parentheses,
 * for example, {@code (x, y, z)}.
 * <p>
 * Differs from {@link SqmJpaCompoundSelection} in that this node may occur
 * anywhere in the SQM tree, whereas {@code SqmJpaCompoundSelection} is only
 * legal in the {@code SELECT} clause.
 *
 * @author Steve Ebersole
 */
public class SqmTuple<T>
		extends AbstractSqmExpression<T>
		implements JpaCompoundSelection<T> {
	private final List<SqmExpression<?>> groupedExpressions;

	public SqmTuple(NodeBuilder nodeBuilder, SqmExpression<?>... groupedExpressions) {
		this( Arrays.asList( groupedExpressions ), nodeBuilder );
	}

	public SqmTuple(NodeBuilder nodeBuilder, SqmExpressible<T> type, SqmExpression<?>... groupedExpressions) {
		this( Arrays.asList( groupedExpressions ), type, nodeBuilder );
	}

	public SqmTuple(List<SqmExpression<?>> groupedExpressions, NodeBuilder nodeBuilder) {
		this( groupedExpressions, null, nodeBuilder );
	}

	public SqmTuple(List<SqmExpression<?>> groupedExpressions, SqmExpressible<T> type, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		if ( groupedExpressions.isEmpty() ) {
			throw new SemanticException( "Tuple constructor must have at least one element" );
		}
		this.groupedExpressions = groupedExpressions;
		if ( type == null ) {
			setExpressibleType( nodeBuilder.getTypeConfiguration().resolveTupleType( groupedExpressions ) );
		}
	}

	@Override
	public SqmTuple<T> copy(SqmCopyContext context) {
		final SqmTuple<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmExpression<?>> groupedExpressions = new ArrayList<>( this.groupedExpressions.size() );
		for ( SqmExpression<?> groupedExpression : this.groupedExpressions ) {
			groupedExpressions.add( groupedExpression.copy( context ) );
		}
		final SqmTuple<T> expression = context.registerCopy(
				this,
				new SqmTuple<>( groupedExpressions, getNodeType(), nodeBuilder() )
		);
		copyTo( expression, context );
		return expression;
	}

	public List<SqmExpression<?>> getGroupedExpressions() {
		return groupedExpressions;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTuple( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( '(' );
		groupedExpressions.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < groupedExpressions.size(); i++ ) {
			hql.append(", ");
			groupedExpressions.get( i ).appendHqlString( hql, context );
		}
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTuple<?> that
			&& Objects.equals( this.groupedExpressions, that.groupedExpressions );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( groupedExpressions );
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return getGroupedExpressions();
	}

	@Override
	public Integer getTupleLength() {
		return groupedExpressions.size();
	}
}
