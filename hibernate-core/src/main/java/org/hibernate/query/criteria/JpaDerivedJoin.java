/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Christian Beikov
 */
@Incubating
public interface JpaDerivedJoin<T> extends JpaDerivedFrom<T>, JpaJoin<T,T> {
	/**
	 * Specifies whether the subquery part can access previous from node aliases.
	 * Normally, subqueries in the from clause are unable to access other from nodes,
	 * but when specifying them as lateral, they are allowed to do so.
	 * Refer to the SQL standard definition of LATERAL for more details.
	 */
	boolean isLateral();

	@Override
	JpaDerivedJoin<T> on(@Nullable JpaExpression<Boolean> restriction);

	@Override
	JpaDerivedJoin<T> on(@Nullable Expression<Boolean> restriction);

	@Override
	JpaDerivedJoin<T> on(JpaPredicate @Nullable... restrictions);

	@Override
	JpaDerivedJoin<T> on(Predicate @Nullable... restrictions);

}
