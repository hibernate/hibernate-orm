/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import java.util.List;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

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

	/**
	 * Set the join restriction.
	 */
	@Override
	@Nonnull
	JpaFunctionJoin<E> on(@Nullable JpaExpression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaFunctionJoin<E> on(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Override
	@Nonnull
	JpaFunctionJoin<E> on(@Nullable JpaPredicate... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaFunctionJoin<E> on(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaFunctionJoin<E> on(@Nonnull List<? extends Expression<Boolean>> restrictions);
}
