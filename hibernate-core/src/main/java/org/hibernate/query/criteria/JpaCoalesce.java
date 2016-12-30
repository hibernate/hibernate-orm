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
public interface JpaCoalesce<T> extends CriteriaBuilder.Coalesce<T>, JpaExpressionImplementor<T> {
	@Override
	JpaCoalesce<T> value(T value);

	@Override
	JpaCoalesce<T> value(Expression<? extends T> value);
}
