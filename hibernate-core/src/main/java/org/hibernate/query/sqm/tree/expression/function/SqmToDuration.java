/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

import static java.util.Arrays.asList;
import static org.hibernate.query.UnaryArithmeticOperator.UNARY_MINUS;

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
		return null;
	}

	private void illegalConversion(SqmExtractUnit<?> unit) {
		throw new SemanticException("illegal unit conversion " + this.unit.getUnitName() + " to " + unit.getUnitName());
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {}

	@Override
	public SqmExpression<?> evaluateDuration(
			QueryEngine queryEngine,
			SqmExtractUnit<?> unit,
			BasicValuedExpressableType<Long> resultType,
			NodeBuilder nodeBuilder) {
		String fromUnit = this.unit.getUnitName();
		String toUnit = unit.getUnitName();
		if ( toUnit.equals(fromUnit) ) {
			return magnitude;
		}
		long factor = 1;
		switch (toUnit) {
			case "second":
				switch (fromUnit) {
					case "day":
						factor*=24;
					case "hour":
						factor*=60;
					case "minute":
						factor*=60;
					break;
					default: illegalConversion(unit);
				}
				break;
			case "minute":
				switch (fromUnit) {
					case "second":
						factor/=60;
					break;
					case "day":
						factor*=24;
					case "hour":
						factor*=60;
					break;
					default: illegalConversion(unit);
				}
				break;
			case "hour":
				switch (fromUnit) {
					case "second":
						factor/=60;
					case "minute":
						factor/=60;
					break;
					case "day":
						factor*=24;
					break;
					default: illegalConversion(unit);
				}
				break;
			case "day":
				switch (fromUnit) {
					case "second":
						factor/=60;
					case "minute":
						factor/=60;
					case "hour":
						factor/=24;
					break;
					default: illegalConversion(unit);
				}
				break;
			case "month":
				switch (fromUnit) {
					case "year":
						factor*=12;
					break;
					default: illegalConversion(unit);
				}
				break;
			case "year":
				switch (fromUnit) {
					case "month":
						factor/=12;
						break;
					default: illegalConversion(unit);
				}
				break;
			default: illegalConversion(unit);
		}
		return new SqmBinaryArithmetic<>(
				BinaryArithmeticOperator.MULTIPLY,
				new SqmLiteral<>( factor, resultType, nodeBuilder ),
				magnitude,
				resultType,
				nodeBuilder
		);
	}

	@Override
	public SqmExpression<?> evaluateDurationAddition(
			boolean negate,
			SqmExpression<?> timestamp,
			QueryEngine queryEngine,
			NodeBuilder nodeBuilder) {

		SqmExpression<?> magnitude = this.magnitude;
		if ( negate ) {
			magnitude = new SqmUnaryOperation<>( UNARY_MINUS, magnitude );
		}

		return queryEngine.getSqmFunctionRegistry().findFunctionTemplate("timestampadd").makeSqmFunctionExpression(
				asList( unit, magnitude, timestamp ),
				(AllowableFunctionReturnType<?>) timestamp.getExpressableType(),
				queryEngine
		);
	}

	@Override
	public String asLoggableText() {
		return magnitude.asLoggableText() + " " + unit.getUnitName();
	}
}


