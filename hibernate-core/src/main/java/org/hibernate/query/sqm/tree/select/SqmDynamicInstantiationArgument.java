/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

/**
 * Represents an individual argument to a dynamic instantiation.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiationArgument implements SqmAliasedNode {
	private final SqmSelectableNode selectableNode;
	private final String alias;

	public SqmDynamicInstantiationArgument(
			SqmSelectableNode selectableNode,
			String alias) {
		this.selectableNode = selectableNode;
		this.alias = alias;
	}

	@Override
	public SqmSelectableNode getSelectableNode() {
		return selectableNode;
	}

	public String getAlias() {
		return alias;
	}
}
