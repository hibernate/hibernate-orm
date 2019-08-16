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

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.internal.SqmExpressionInterpretation;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

/**
 * The base contract for any kind of expression node in the SQM tree.
 * An expression might be a reference to an attribute, a literal,
 * a function, etc.
 *
 * @param <T> The Java type of the expression
 *
 * @author Steve Ebersole
 */
public interface SqmExpression<T> extends SqmSelectableNode<T>, JpaExpression<T>, SqmExpressionInterpretation<T> {
	/**
	 * The expression's type.
	 *
	 * Can change as a result of calls to {@link #applyInferableType}
	 */
	@Override
	SqmExpressable<T> getNodeType();

	/**
	 * Used to apply type information based on the expression's usage
	 * within the query.
	 *
	 * @apiNote The SqmExpressable type parameter is dropped here because
	 * the inference could technically cause a change in Java type (i.e.
	 * an implicit cast)
	 */
	void applyInferableType(SqmExpressable<?> type);

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

	default <X> SqmExpression<X> castAs(DomainType<X> type) {
		return nodeBuilder().getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( "cast" )
				.makeSqmFunctionExpression( this, ( AllowableFunctionReturnType<X>) type, nodeBuilder().getQueryEngine() );
	}


}
