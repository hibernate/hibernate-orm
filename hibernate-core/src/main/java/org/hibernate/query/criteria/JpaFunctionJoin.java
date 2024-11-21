/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @since 7.0
 */
@Incubating
public interface JpaFunctionJoin<E> extends JpaFunctionFrom<Object, E>, JpaJoin<Object, E> {
	/**
	 * Specifies whether the function arguments can refer to previous from node aliases.
	 * Normally, functions in the from clause are unable to access other from nodes,
	 * but when specifying them as lateral, they are allowed to do so.
	 * Refer to the SQL standard definition of LATERAL for more details.
	 */
	boolean isLateral();

	@Override
	JpaFunctionJoin<E> on(JpaExpression<Boolean> restriction);

	@Override
	JpaFunctionJoin<E> on(Expression<Boolean> restriction);

	@Override
	JpaFunctionJoin<E> on(JpaPredicate... restrictions);

	@Override
	JpaFunctionJoin<E> on(Predicate... restrictions);

}
