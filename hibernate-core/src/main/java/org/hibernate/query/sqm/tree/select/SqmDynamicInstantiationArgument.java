/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.NodeBuilder;

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
	public SqmSelectableNode<T> getSelectableNode() {
		return selectableNode;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}
}
