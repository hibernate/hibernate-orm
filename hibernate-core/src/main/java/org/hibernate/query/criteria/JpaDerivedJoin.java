/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

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
	@Nonnull
	JpaDerivedJoin<T> on(@Nullable JpaExpression<Boolean> restriction);

	@Nonnull
	@Override
	JpaDerivedJoin<T> on(@Nonnull Expression<Boolean> restriction);

	@Override
	@Nonnull
	JpaDerivedJoin<T> on(JpaPredicate @Nullable... restrictions);

	@Nonnull
	@Override
	JpaDerivedJoin<T> on(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	JpaDerivedJoin<T> on(@Nonnull List<? extends Expression<Boolean>> restrictions);
}
