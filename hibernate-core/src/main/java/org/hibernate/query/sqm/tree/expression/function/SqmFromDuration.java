/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Gavin King
 */
public class SqmFromDuration<T> extends AbstractSqmNode implements SqmTypedNode<T>, SqmVisitableNode {
	private final SqmExpression<?> duration;
	private final SqmExtractUnit<?> unit;
	private final AllowableFunctionReturnType type;

	public SqmFromDuration(
			SqmExpression<?> duration,
			SqmExtractUnit<?> unit,
			AllowableFunctionReturnType<T> type,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.duration = duration;
		this.unit = unit;
		this.type = type;
	}

	public SqmExpression<?> getDuration() {
		return duration;
	}

	public SqmExtractUnit<?> getUnit() {
		return unit;
	}

	public AllowableFunctionReturnType<T> getType() {
		return type;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return null;
	}

	@Override
	public ExpressableType getExpressableType() {
		return type;
	}
}


