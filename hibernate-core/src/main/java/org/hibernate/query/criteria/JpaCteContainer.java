/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Collection;
import java.util.function.Function;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * Common contract for criteria parts that can hold CTEs (common table expressions).
 */
@Incubating
public interface JpaCteContainer extends JpaCriteriaNode {

	/**
	 * Returns the CTEs that are registered on this container.
	 */
	@Nonnull
	Collection<? extends JpaCteCriteria<?>> getCteCriterias();

	/**
	 * Returns a CTE that is registered by the given name on this container, or any of its parents.
	 */
	@Nullable
	<T> JpaCteCriteria<T> getCteCriteria(@Nonnull String cteName);

	/**
	 * Registers the given {@link CriteriaQuery} and returns a {@link JpaCteCriteria},
	 * which can be used for querying.
	 *
	 * @see JpaCriteriaQuery#from(JpaCteCriteria)
	 *
	 * @deprecated Use {@link #with(String, AbstractQuery)} and provide an explicit
	 *             name for the CTE
	 */
	@Deprecated(since = "7", forRemoval = true)
	@Nonnull
	<T> JpaCteCriteria<T> with(@Nonnull AbstractQuery<T> criteria);

	/**
	 * Allows to register a recursive CTE. The base {@link CriteriaQuery} serves
	 * for the structure of the {@link JpaCteCriteria}, which is made available in the recursive criteria producer function,
	 * so that the recursive {@link CriteriaQuery} is able to refer to the CTE again.
	 *
	 * @see JpaCriteriaQuery#from(JpaCteCriteria)
	 */
	@Nonnull
	<T> JpaCteCriteria<T> withRecursiveUnionAll(@Nonnull AbstractQuery<T> baseCriteria, @Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);

	/**
	 * Allows to register a recursive CTE. The base {@link CriteriaQuery} serves
	 * for the structure of the {@link JpaCteCriteria}, which is made available in the recursive criteria producer function,
	 * so that the recursive {@link CriteriaQuery} is able to refer to the CTE again.
	 *
	 * @see JpaCriteriaQuery#from(JpaCteCriteria)
	 */
	@Nonnull
	<T> JpaCteCriteria<T> withRecursiveUnionDistinct(@Nonnull AbstractQuery<T> baseCriteria, @Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);

	/**
	 * Like {@link #with(AbstractQuery)} but assigns an explicit CTE name.
	 */
	@Nonnull
	<T> JpaCteCriteria<T> with(@Nonnull String name, @Nonnull AbstractQuery<T> criteria);

	/**
	 * Like {@link #withRecursiveUnionAll(AbstractQuery, Function)} but assigns an explicit CTE name.
	 */
	@Nonnull
	<T> JpaCteCriteria<T> withRecursiveUnionAll(@Nonnull String name, @Nonnull AbstractQuery<T> baseCriteria, @Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);

	/**
	 * Like {@link #withRecursiveUnionDistinct(AbstractQuery, Function)} but assigns an explicit CTE name.
	 */
	@Nonnull
	<T> JpaCteCriteria<T> withRecursiveUnionDistinct(@Nonnull String name, @Nonnull AbstractQuery<T> baseCriteria, @Nonnull Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);
}
