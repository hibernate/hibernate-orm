/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import jakarta.annotation.Nullable;
import org.hibernate.query.common.FetchClauseType;

/**
 * Models a {@code SELECT} query.  Used as a delegate in
 * implementing {@link jakarta.persistence.criteria.CriteriaQuery}
 * and {@link jakarta.persistence.criteria.Subquery}.
 *
 * @apiNote Internally (HQL and SQM) Hibernate supports ordering and limiting
 * for both root- and sub- criteria even though JPA only defines support for
 * them on a root.
 *
 * @see JpaCriteriaQuery
 * @see JpaSubQuery
 *
 * @author Steve Ebersole
 */
public interface JpaQueryStructure<T> extends JpaQueryPart<T> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select clause

	/**
	 * Return whether duplicate query results are eliminated.
	 */
	boolean isDistinct();

	/**
	 * Set whether duplicate query results are eliminated.
	 */
	@Nonnull
	JpaQueryStructure<T> setDistinct(boolean distinct);

	/**
	 * Return the query selection.
	 */
	JpaSelection<T> getSelection();

	/**
	 * Set the query selection.
	 */
	@Nonnull
	JpaQueryStructure<T> setSelection(JpaSelection<T> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// From clause

	/**
	 * Return the query roots.
	 */
	Set<? extends JpaRoot<?>> getRoots();

	/**
	 * Return the query roots as a list.
	 */
	List<? extends JpaRoot<?>> getRootList();

	/**
	 * Add a query root.
	 */
	@Nonnull
	JpaQueryStructure<T> addRoot(JpaRoot<?> root);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Where clause

	/**
	 * Set the restriction.
	 */
	@Nullable JpaPredicate getRestriction();

	@Nonnull
	JpaQueryStructure<T> setRestriction(@Nullable JpaPredicate restriction);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setRestriction(@Nullable Expression<Boolean> restriction);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setRestriction(BooleanExpression... restrictions);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setRestriction(Predicate... restrictions);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setRestriction(List<Predicate> restrictions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping (group-by / having) clause

	/**
	 * Return the grouping expressions.
	 */
	@Nonnull
	List<? extends JpaExpression<?>> getGroupingExpressions();

	/**
	 * Set the grouping expressions.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupingExpressions(List<? extends Expression<?>> grouping);

	/**
	 * Set the grouping expressions.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupingExpressions(Expression<?>... grouping);

	/**
	 * Return the group restriction.
	 */
	@Nullable
	JpaPredicate getGroupRestriction();

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(@Nullable JpaPredicate restrictions);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(@Nullable Expression<Boolean> restriction);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(Predicate... restrictions);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(BooleanExpression... restrictions);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(List<Predicate> restrictions);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	/**
	 * Set the ordering specifications.
	 */
	@Override
	@Nonnull
	JpaQueryStructure<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);

	/**
	 * Set the query offset.
	 */
	@Override
	@Nonnull
	JpaQueryStructure<T> setOffset(@Nullable JpaExpression<? extends Number> offset);

	/**
	 * Set the query fetch limit.
	 */
	@Override
	@Nonnull
	JpaQueryStructure<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	/**
	 * Set the query fetch limit.
	 */
	@Override
	@Nonnull
	JpaQueryStructure<T> setFetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);
}
