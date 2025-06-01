/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * Models a reference to a {@link org.hibernate.query.sqm.tree.select.SqmAliasedNode}
 * used in the order-by or group-by clause by either position or alias,
 * though the reference is normalized here to a positional ref
 */
public class SqmAliasedNodeRef extends AbstractSqmExpression<Integer> {

	private final int position;
	// The navigable path is optionally set in case this is a reference to an attribute of a selection
	private final NavigablePath navigablePath;

	public SqmAliasedNodeRef(int position, SqmBindableType<Integer> intType, NodeBuilder criteriaBuilder) {
		super( intType, criteriaBuilder );
		this.position = position;
		this.navigablePath = null;
	}

	public SqmAliasedNodeRef(
			int position,
			NavigablePath navigablePath,
			SqmBindableType<Integer> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.position = position;
		this.navigablePath = navigablePath;
	}

	private SqmAliasedNodeRef(SqmAliasedNodeRef original) {
		super( original.getNodeType(), original.nodeBuilder() );
		this.position = original.position;
		this.navigablePath = original.navigablePath;
	}

	@Override
	public SqmAliasedNodeRef copy(SqmCopyContext context) {
		final SqmAliasedNodeRef existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAliasedNodeRef expression = context.registerCopy( this, new SqmAliasedNodeRef( this ) );
		copyTo( expression, context );
		return expression;
	}

	public int getPosition() {
		return position;
	}

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		// we expect this to be handled specially in
		// `BaseSqmToSqlAstConverter#resolveGroupOrOrderByExpression`
		throw new UnsupportedOperationException();
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( navigablePath == null ) {
			hql.append( position );
		}
		else {
			hql.append( navigablePath.getLocalName() );
		}
	}
}
