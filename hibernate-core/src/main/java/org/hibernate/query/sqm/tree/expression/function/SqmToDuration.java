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
		throw new SemanticException("illegal unit conversion " + this.unit.getUnit() + " to " + unit.getUnit());
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {}

	@Override
	public SqmExpression<?> evaluateDuration(
			QueryEngine queryEngine,
			SqmExtractUnit<?> unit,
			BasicValuedExpressableType<Long> resultType,
			NodeBuilder nodeBuilder) {
		TemporalUnit fromUnit = this.unit.getUnit();
		TemporalUnit toUnit = unit.getUnit();
		if ( toUnit.equals(fromUnit) ) {
			return magnitude;
		}
		long factor = 1;
		//TODO: MICROSECOND!
		switch (toUnit) {
			case MILLISECOND:
				switch (fromUnit) {
					case WEEK:
						factor*=7;
					case DAY:
						factor*=24;
					case HOUR:
						factor*=60;
					case MINUTE:
						factor*=60;
						break;
					case SECOND:
						factor*=1e3;
						break;
					default: illegalConversion(unit);
				}
				break;
			case SECOND:
				switch (fromUnit) {
					case MILLISECOND:
						factor/=1e3;
					break;
					case WEEK:
						factor*=7;
					case DAY:
						factor*=24;
					case HOUR:
						factor*=60;
					case MINUTE:
						factor*=60;
					break;
					default: illegalConversion(unit);
				}
				break;
			case MINUTE:
				switch (fromUnit) {
					case MILLISECOND:
						factor/=1e3;
					case SECOND:
						factor/=60;
					break;
					case WEEK:
						factor*=7;
					case DAY:
						factor*=24;
					case HOUR:
						factor*=60;
					break;
					default: illegalConversion(unit);
				}
				break;
			case HOUR:
				switch (fromUnit) {
					case MILLISECOND:
						factor/=1e3;
					case SECOND:
						factor/=60;
					case MINUTE:
						factor/=60;
					break;
					case WEEK:
						factor*=7;
					case DAY:
						factor*=24;
					break;
					default: illegalConversion(unit);
				}
				break;
			case DAY:
				switch (fromUnit) {
					case MILLISECOND:
						factor/=1e3;
					case SECOND:
						factor/=60;
					case MINUTE:
						factor/=60;
					case HOUR:
						factor/=24;
					break;
					case WEEK:
						factor*=7;
						break;
					default: illegalConversion(unit);
				}
				break;
			case WEEK:
				switch (fromUnit) {
					case MILLISECOND:
						factor/=1e3;
					case SECOND:
						factor/=60;
					case MINUTE:
						factor/=60;
					case HOUR:
						factor/=24;
					case DAY:
						factor/=7;
						break;
					default: illegalConversion(unit);
				}
			case MONTH:
				switch (fromUnit) {
					case YEAR:
						factor*=4;
					case QUARTER:
						factor*=3;
					break;
					default: illegalConversion(unit);
				}
				break;
			case QUARTER:
				switch (fromUnit) {
					case MONTH:
						factor*=3;
						break;
					case YEAR:
						factor/=4;
						break;
					default: illegalConversion(unit);
				}
				break;
			case YEAR:
				switch (fromUnit) {
					case MONTH:
						factor/=3;
					case QUARTER:
						factor/=4;
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
		return magnitude.asLoggableText() + " " + unit.getUnit();
	}
}


