/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Gavin King
 */
public class SqmByUnit extends AbstractSqmExpression<Long> {
	private final SqmExtractUnit<?> unit;
	private final SqmExpression<?> duration;

	public SqmByUnit(
			SqmExtractUnit<?> unit,
			SqmExpression<?> duration,
			BasicValuedExpressableType<Long> longType,
			NodeBuilder nodeBuilder) {
		super( longType, nodeBuilder );
		this.unit = unit;
		this.duration = duration;
	}

	public SqmExtractUnit<?> getUnit() {
		return unit;
	}

	public SqmExpression<?> getDuration() {
		return duration;
	}

	@Override
	public BasicValuedExpressableType<Long> getExpressableType() {
		return (BasicValuedExpressableType<Long>) super.getExpressableType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitByUnit( this );
	}
}
