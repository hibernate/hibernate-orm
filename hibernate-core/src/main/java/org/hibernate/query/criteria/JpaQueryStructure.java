/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;
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

	JpaQueryStructure<T> setDistinct(boolean distinct);

	JpaSelection<T> getSelection();

	JpaQueryStructure<T> setSelection(JpaSelection<T> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// From clause

	Set<? extends JpaRoot<?>> getRoots();

	List<? extends JpaRoot<?>> getRootList();

	JpaQueryStructure<T> addRoot(JpaRoot<?> root);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Where clause

	@Nullable JpaPredicate getRestriction();

	JpaQueryStructure<T> setRestriction(@Nullable JpaPredicate restriction);

	JpaQueryStructure<T> setRestriction(@Nullable Expression<Boolean> restriction);

	JpaQueryStructure<T> setRestriction(Predicate... restrictions);

	JpaQueryStructure<T> setRestriction(List<Predicate> restrictions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping (group-by / having) clause

	List<? extends JpaExpression<?>> getGroupingExpressions();

	JpaQueryStructure<T> setGroupingExpressions(List<? extends Expression<?>> grouping);

	JpaQueryStructure<T> setGroupingExpressions(Expression<?>... grouping);

	@Nullable JpaPredicate getGroupRestriction();

	JpaQueryStructure<T> setGroupRestriction(@Nullable JpaPredicate restrictions);

	JpaQueryStructure<T> setGroupRestriction(@Nullable Expression<Boolean> restriction);

	JpaQueryStructure<T> setGroupRestriction(Predicate... restrictions);

	JpaQueryStructure<T> setGroupRestriction(List<Predicate> restrictions);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	JpaQueryStructure<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);

	@Override
	JpaQueryStructure<T> setOffset(@Nullable JpaExpression<? extends Number> offset);

	@Override
	JpaQueryStructure<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	@Override
	JpaQueryStructure<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);
}
