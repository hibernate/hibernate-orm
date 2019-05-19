/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.function.Consumer;
import javax.persistence.criteria.Expression;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * The base contract for any kind of expression node in the SQM tree.
 * An expression might be a reference to an attribute, a literal,
 * a function, etc.
 *
 * @author Steve Ebersole
 */
public interface SqmExpression<T> extends SqmSelectableNode<T>, JpaExpression<T> {
	/**
	 * The expression's type.
	 *
	 * Can change as a result of calls to {@link #applyInferableType}
	 */
	ExpressableType<T> getExpressableType();

	/**
	 * Used to apply type information based on the expression's usage
	 * within the query.
	 */
	void applyInferableType(ExpressableType<?> type);

	@Override
	default void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		jpaSelectionConsumer.accept( this );
	}

	@Override
	SqmExpression<Long> asLong();

	@Override
	SqmExpression<Integer> asInteger();

	@Override
	SqmExpression<Float> asFloat();

	@Override
	SqmExpression<Double> asDouble();

	@Override
	SqmExpression<BigDecimal> asBigDecimal();

	@Override
	SqmExpression<BigInteger> asBigInteger();

	@Override
	SqmExpression<String> asString();

	@Override
	<X> SqmExpression<X> as(Class<X> type);

	@Override
	SqmPredicate isNull();

	@Override
	SqmPredicate isNotNull();

	@Override
	SqmPredicate in(Object... values);

	@Override
	SqmPredicate in(Expression<?>... values);

	@Override
	SqmPredicate in(Collection<?> values);

	@Override
	SqmPredicate in(Expression<Collection<?>> values);

	default <X> SqmExpression<X> castAs(AllowableFunctionReturnType<X> type) {
		return nodeBuilder().getQueryEngine().getSqmFunctionRegistry().findFunctionTemplate( "cast" )
				.makeSqmFunctionExpression( this, type, nodeBuilder().getQueryEngine() );
	}

	/**
	 * Apply an 'of unit' operator to a branch of the
	 * expression tree, producing a complex expression
	 * involving binary operators applied to numbers
	 *
	 * @return an expression with 'of unit' occurring
	 *         only for leaf timestamp/date subtractions
	 */
	default SqmExpression<?> evaluateDuration(
			QueryEngine queryEngine,
			SqmExtractUnit<?> unit,
			BasicValuedExpressableType<Long> resultType,
			NodeBuilder nodeBuilder) {
		return this;
	}

	default SqmExpression<?> evaluateDurationAddition(
			boolean negate,
			SqmExpression<?> timestamp,
			QueryEngine queryEngine,
			NodeBuilder nodeBuilder) {
		return this;
	}
}
