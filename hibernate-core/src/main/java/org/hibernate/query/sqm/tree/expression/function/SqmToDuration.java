/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;

/**
 * @author Gavin King
 */
public class SqmToDuration<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> magnitude;
	private final SqmExtractUnit<?> unit;

	public SqmToDuration(
			SqmExpression<?> magnitude,
			SqmExtractUnit<?> unit,
			AllowableFunctionReturnType<T> type,
			NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		this.magnitude = magnitude;
		this.unit = unit;
	}

	public SqmExpression<?> getMagnitude() {
		return magnitude;
	}

	public SqmExtractUnit<?> getUnit() {
		return unit;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitToDuration( this );
	}

	@Override
	public BasicValuedExpressableType<T> getExpressableType() {
		return (BasicValuedExpressableType<T>) super.getExpressableType();
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {}

	@Override
	public String asLoggableText() {
		return magnitude.asLoggableText() + " " + unit.getUnit();
	}
}


