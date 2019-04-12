/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Subquery;

/**
 * @author Steve Ebersole
 */
public interface JpaSubQuery<T> extends Subquery<T>, JpaSelectCriteria<T>, JpaExpression<T> {
	@Override
	JpaSubQuery<T> distinct(boolean distinct);

	@Override
	JpaExpression<T> getSelection();

	@Override
	JpaSubQuery<T> select(Expression<T> expression);

	@Override
	JpaSubQuery<T> where(Expression<Boolean> restriction);

	@Override
	JpaSubQuery<T> where(Predicate... restrictions);

	@Override
	JpaSubQuery<T> groupBy(Expression<?>... grouping);

	@Override
	JpaSubQuery<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaSubQuery<T> having(Expression<Boolean> restriction);

	@Override
	JpaSubQuery<T> having(Predicate... restrictions);
}
