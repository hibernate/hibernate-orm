/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.Incubating;
import org.hibernate.sqm.parser.criteria.tree.JpaExpression;

/**
 * Hibernate ORM specialization of the JPA {@link Expression} contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaExpressionImplementor<T> extends JpaSelectionImplementor<T>, JpaExpression<T> {

	@Override
	JpaExpressionImplementor<Long> asLong();

	@Override
	JpaExpressionImplementor<Integer> asInteger();

	@Override
	JpaExpressionImplementor<Float> asFloat();

	@Override
	JpaExpressionImplementor<Double> asDouble();

	@Override
	JpaExpressionImplementor<BigDecimal> asBigDecimal();

	@Override
	JpaExpressionImplementor<BigInteger> asBigInteger();

	@Override
	JpaExpressionImplementor<String> asString();

	@Override
	JpaPredicateImplementor isNull();

	@Override
	JpaPredicateImplementor isNotNull();

	@Override
	JpaPredicateImplementor in(Object... values);

	@Override
	JpaPredicateImplementor in(Expression<?>[] values);

	@Override
	JpaPredicateImplementor in(Collection<?> values);

	@Override
	JpaPredicateImplementor in(Expression<Collection<?>> values);

	@Override
	<X> JpaExpressionImplementor<X> as(Class<X> type);

	@Override
	JpaSelectionImplementor<T> alias(String name);
}
