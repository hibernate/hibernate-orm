/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Christian Beikov
 */
public class SqmSummarization<T> extends AbstractSqmExpression<T> {

	private final Kind kind;
	private final List<SqmExpression<?>> groupings;

	public SqmSummarization(Kind kind, List<SqmExpression<?>> groupings, NodeBuilder criteriaBuilder) {
		super( null, criteriaBuilder );
		this.kind = kind;
		this.groupings = groupings;
	}

	@Override
	public SqmSummarization<T> copy(SqmCopyContext context) {
		final SqmSummarization<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmExpression<?>> groupings = new ArrayList<>( this.groupings.size() );
		for ( SqmExpression<?> grouping : this.groupings ) {
			groupings.add( grouping.copy( context ) );
		}
		final SqmSummarization<T> expression = context.registerCopy(
				this,
				new SqmSummarization<>(
						kind,
						groupings,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public Kind getKind() {
		return kind;
	}

	public List<SqmExpression<?>> getGroupings() {
		return groupings;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSummarization( this );
	}

	public enum Kind {
		ROLLUP,
		CUBE
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( kind );
		hql.append( " (" );
		groupings.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < groupings.size(); i++ ) {
			hql.append(", ");
			groupings.get( i ).appendHqlString( hql, context );
		}
		hql.append( ')' );
	}

}
