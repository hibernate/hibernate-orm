/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.JpaSubQuery;

/**
 * @author Steve Ebersole
 */
public interface SubQuery<T> extends SelectCriteriaImplementor<T>, JpaSubQuery<T> {
	@Override
	SubQuery<T> distinct(boolean distinct);

	@Override
	ExpressionImplementor<T> getSelection();

	@Override
	SubQuery<T> select(Expression<T> expression);

	@Override
	SubQuery<T> where(Expression<Boolean> restriction);

	@Override
	SubQuery<T> where(Predicate... restrictions);

	@Override
	SubQuery<T> groupBy(Expression<?>... grouping);

	@Override
	SubQuery<T> groupBy(List<Expression<?>> grouping);

	@Override
	SubQuery<T> having(Expression<Boolean> restriction);

	@Override
	SubQuery<T> having(Predicate... restrictions);
}
