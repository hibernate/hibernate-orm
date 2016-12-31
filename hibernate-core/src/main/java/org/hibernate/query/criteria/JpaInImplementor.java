/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.hibernate.Incubating;

/**
 * Hibernate ORM specialization of the JPA {@link CriteriaBuilder.In}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaInImplementor<T> extends CriteriaBuilder.In<T>, JpaPredicateImplementor {
	@Override
	JpaExpressionImplementor<T> getExpression();

	@Override
	JpaInImplementor<T> value(T value);

	@Override
	JpaInImplementor<T> value(Expression<? extends T> value);
}
