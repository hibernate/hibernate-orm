/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * Represents an individual selection within a select clause.
 *
 * @author Steve Ebersole
 */
public class SqmSelection<T> extends AbstractSqmNode implements SqmAliasedNode<T> {
	private final SqmSelectableNode<T> selectableNode;
	private final String alias;

	public SqmSelection(
			SqmSelectableNode<T> selectableNode,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert selectableNode != null;
		this.selectableNode = selectableNode;
		this.alias = selectableNode.getAlias();
	}

	public SqmSelection(
			SqmSelectableNode<T> selectableNode,
			String alias,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert selectableNode != null;
		this.selectableNode = selectableNode;
		this.alias = alias;
		selectableNode.alias( alias );
	}

	@Override
	public SqmSelection<T> copy(SqmCopyContext context) {
		return new SqmSelection<>( selectableNode.copy( context ), alias, nodeBuilder() );
	}

	@Override
	public SqmSelectableNode<T> getSelectableNode() {
		return selectableNode;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSelection( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		selectableNode.appendHqlString( hql, context );
		if ( alias != null ) {
			hql.append( " as " ).append( alias );
		}
	}


	@Override
	public boolean equals(Object object) {
		return object instanceof SqmSelection<?> that
			&& Objects.equals( this.selectableNode, that.selectableNode )
			&& Objects.equals( this.alias, that.alias );
	}

	@Override
	public int hashCode() {
		return Objects.hash( selectableNode, alias );
	}
}
