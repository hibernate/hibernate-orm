/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

/**
 * @author Gavin King
 */
public class SqmExtractUnit<T> extends AbstractSqmNode implements SqmTypedNode<T>, SqmVisitableNode {
	private String name;
	private AllowableFunctionReturnType type;

	public SqmExtractUnit(String name, AllowableFunctionReturnType<T> type, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.type = type;
		this.name = name;
	}

	public AllowableFunctionReturnType getType() {
		return type;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitExtractUnit(this);
	}

	public String getUnitName() {
		return name;
	}

	@Override
	public SqmExpressable<T> getNodeType() {
		return type;
	}
}


