/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaSearchedCase<T> extends JpaExpression<T>, CriteriaBuilder.Case<T> {
	@Override
	JpaSearchedCase<T> when(Expression<Boolean> condition, T result);

	@Override
	JpaSearchedCase<T> when(Expression<Boolean> condition, Expression<? extends T> result);

	@Override
	JpaExpression<T> otherwise(T result);

	@Override
	JpaExpression<T> otherwise(Expression<? extends T> result);
}
