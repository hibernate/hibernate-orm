/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;

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
	public void appendHqlString(StringBuilder sb) {
		selectableNode.appendHqlString( sb );
		if ( alias != null ) {
			sb.append( " as " ).append( alias );
		}
	}
}
