/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.NodeBuilder;

/**
 * Base implementation of a criteria node.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmNode implements SqmNode {
	private final NodeBuilder builder;

	protected AbstractSqmNode(NodeBuilder builder) {
		this.builder = builder;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return builder;
	}
}
