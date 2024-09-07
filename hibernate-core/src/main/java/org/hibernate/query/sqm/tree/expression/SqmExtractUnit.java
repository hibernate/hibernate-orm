/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

/**
 * @author Gavin King
 */
public class SqmExtractUnit<T> extends AbstractSqmNode implements SqmTypedNode<T> {
	private final TemporalUnit unit;
	private final ReturnableType<T> type;

	public SqmExtractUnit(TemporalUnit unit, ReturnableType<T> type, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.unit = unit;
		this.type = type;
	}

	@Override
	public SqmExtractUnit<T> copy(SqmCopyContext context) {
		return this;
	}

	public TemporalUnit getUnit() {
		return unit;
	}

	public ReturnableType<T> getType() {
		return type;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitExtractUnit( this );
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return type;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( unit );
	}
}


