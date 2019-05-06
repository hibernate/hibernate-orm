/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

/**
 * Represents an individual selection within a select clause.
 *
 * @author Steve Ebersole
 */
public class SqmSelection<T> extends AbstractSqmNode implements SqmAliasedNode<T>, SqmVisitableNode {
	private final SqmSelectableNode<T> selectableNode;

	public SqmSelection(
			SqmSelectableNode<T> selectableNode,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.selectableNode = selectableNode;
	}

	public SqmSelection(
			SqmSelectableNode<T> selectableNode,
			String alias,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.selectableNode = selectableNode;
		selectableNode.alias( alias );
	}

	@Override
	public SqmSelectableNode<T> getSelectableNode() {
		return selectableNode;
	}

	@Override
	public String getAlias() {
		// JPA
		return selectableNode.getAlias();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSelection( this );
	}
}
