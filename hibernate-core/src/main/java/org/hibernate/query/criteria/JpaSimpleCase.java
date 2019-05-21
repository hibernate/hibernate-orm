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
public interface JpaSimpleCase<C,R> extends JpaExpression<R>, CriteriaBuilder.SimpleCase<C,R> {
	@Override
	JpaExpression<C> getExpression();

	@Override
	JpaSimpleCase<C, R> when(C condition, R result);

	@Override
	JpaSimpleCase<C, R> when(C condition, Expression<? extends R> result);

	@Override
	JpaSimpleCase<C,R> otherwise(R result);

	@Override
	JpaSimpleCase<C,R> otherwise(Expression<? extends R> result);
}
