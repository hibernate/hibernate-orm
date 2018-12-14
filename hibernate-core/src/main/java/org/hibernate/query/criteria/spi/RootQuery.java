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

import org.hibernate.query.criteria.JpaCriteriaQuery;

/**
 * @author Steve Ebersole
 */
public interface RootQuery<T> extends SelectCriteriaImplementor<T>, JpaCriteriaQuery<T>, Criteria {
	@Override
	RootQuery<T> distinct(boolean distinct);

	@Override
	RootQuery<T> where(Expression<Boolean> restriction);

	@Override
	RootQuery<T> where(Predicate... restrictions);

	@Override
	RootQuery<T> groupBy(Expression<?>... grouping);

	@Override
	RootQuery<T> groupBy(List<Expression<?>> grouping);

	@Override
	RootQuery<T> having(Expression<Boolean> restriction);

	@Override
	RootQuery<T> having(Predicate... restrictions);

}
