/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Gavin King
 */
public class SqmStar extends AbstractSqmExpression<Object> {

	public SqmStar(NodeBuilder builder) {
		super( null, builder );
	}

	@Override
	public SqmStar copy(SqmCopyContext context) {
		final SqmStar existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy( this, new SqmStar( nodeBuilder() ) );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitStar( this );
	}
	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "*" );
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SqmStar;
	}

	@Override
	public int hashCode() {
		return 1;
	}
}
