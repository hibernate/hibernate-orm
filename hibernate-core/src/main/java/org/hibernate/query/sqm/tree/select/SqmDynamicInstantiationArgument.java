/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

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
	public void appendHqlString(StringBuilder sb) {
		selectableNode.appendHqlString( sb );
		if ( alias != null ) {
			sb.append( " as " ).append( alias );
		}
	}
}
