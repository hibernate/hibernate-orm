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
import jakarta.persistence.criteria.Expression;

/**
 * API extension to the JPA {@link Expression} contract
 *
 * @author Steve Ebersole
 */
public interface JpaExpression<T> extends JpaSelection<T>, Expression<T> {

	JpaExpression<Long> asLong();

	JpaExpression<Integer> asInteger();

	JpaExpression<Float> asFloat();

	JpaExpression<Double> asDouble();

	JpaExpression<BigDecimal> asBigDecimal();

	JpaExpression<BigInteger> asBigInteger();

	JpaExpression<String> asString();

	@Override
	<X> JpaExpression<X> as(Class<X> type);

	@Override
	JpaPredicate isNull();

	@Override
	JpaPredicate isNotNull();

	@Override
	JpaPredicate in(Object... values);

	@Override
	JpaPredicate in(Expression<?>... values);

	@Override
	JpaPredicate in(Collection<?> values);

	@Override
	JpaPredicate in(Expression<Collection<?>> values);

	<X> JpaExpression<X> cast(Class<X> type);
}
