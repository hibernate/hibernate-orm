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

import org.checkerframework.checker.nullness.qual.Nullable;
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

	boolean isDistinct();

	@Nonnull
	JpaQueryStructure<T> setDistinct(boolean distinct);

	JpaSelection<T> getSelection();

	@Nonnull
	JpaQueryStructure<T> setSelection(JpaSelection<T> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// From clause

	Set<? extends JpaRoot<?>> getRoots();

	List<? extends JpaRoot<?>> getRootList();

	@Nonnull
	JpaQueryStructure<T> addRoot(JpaRoot<?> root);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Where clause

	@Nullable JpaPredicate getRestriction();

	@Nonnull
	JpaQueryStructure<T> setRestriction(@Nullable JpaPredicate restriction);

	@Nonnull
	JpaQueryStructure<T> setRestriction(@Nullable Expression<Boolean> restriction);

	@Nonnull
	JpaQueryStructure<T> setRestriction(BooleanExpression... restrictions);

	@Nonnull
	JpaQueryStructure<T> setRestriction(Predicate... restrictions);

	@Nonnull
	JpaQueryStructure<T> setRestriction(List<Predicate> restrictions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping (group-by / having) clause

	@Nonnull
	List<? extends JpaExpression<?>> getGroupingExpressions();

	@Nonnull
	JpaQueryStructure<T> setGroupingExpressions(List<? extends Expression<?>> grouping);

	@Nonnull
	JpaQueryStructure<T> setGroupingExpressions(Expression<?>... grouping);

	@Nullable
	JpaPredicate getGroupRestriction();

	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(@Nullable JpaPredicate restrictions);

	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(@Nullable Expression<Boolean> restriction);

	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(Predicate... restrictions);

	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(BooleanExpression... restrictions);

	@Nonnull
	JpaQueryStructure<T> setGroupRestriction(List<Predicate> restrictions);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	@Nonnull
	JpaQueryStructure<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);

	@Override
	@Nonnull
	JpaQueryStructure<T> setOffset(@Nullable JpaExpression<? extends Number> offset);

	@Override
	@Nonnull
	JpaQueryStructure<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	@Override
	@Nonnull
	JpaQueryStructure<T> setFetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);
}
