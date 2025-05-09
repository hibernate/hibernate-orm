/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * Represents an individual argument to a dynamic instantiation.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiationArgument<T> implements SqmAliasedNode<T> {
	private final SqmSelectableNode<T> selectableNode;
	private final String alias;
	private final NodeBuilder nodeBuilder;

	public SqmDynamicInstantiationArgument(
			SqmSelectableNode<T> selectableNode,
			String alias,
			NodeBuilder nodeBuilder) {
		this.selectableNode = selectableNode;
		this.alias = alias;
		this.nodeBuilder = nodeBuilder;
	}

	@Override
	public SqmDynamicInstantiationArgument<T> copy(SqmCopyContext context) {
		return new SqmDynamicInstantiationArgument<>(
				selectableNode.copy( context ),
				alias,
				nodeBuilder
		);
	}

	@Override
	public SqmSelectableNode<T> getSelectableNode() {
		return selectableNode;
	}

	public String getAlias() {
		return alias;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return selectableNode.accept( walker );
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
		return object instanceof SqmDynamicInstantiationArgument<?> that
			&& Objects.equals( selectableNode, that.selectableNode )
			&& Objects.equals( alias, that.alias );
	}

	@Override
	public int hashCode() {
		return Objects.hash( selectableNode, alias );
	}
}
