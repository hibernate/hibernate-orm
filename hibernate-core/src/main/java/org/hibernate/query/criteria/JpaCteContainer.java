/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.Collection;
import java.util.function.Function;

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
	Collection<? extends JpaCteCriteria<?>> getCteCriterias();

	/**
	 * Returns a CTE that is registered by the given name on this container, or any of its parents.
	 */
	<T> JpaCteCriteria<T> getCteCriteria(String cteName);

	/**
	 * Registers the given {@link CriteriaQuery} and returns a {@link JpaCteCriteria},
	 * which can be used for querying.
	 *
	 * @see JpaCriteriaQuery#from(JpaCteCriteria)
	 */
	<T> JpaCteCriteria<T> with(AbstractQuery<T> criteria);

	/**
	 * Allows to register a recursive CTE. The base {@link CriteriaQuery} serves
	 * for the structure of the {@link JpaCteCriteria}, which is made available in the recursive criteria producer function,
	 * so that the recursive {@link CriteriaQuery} is able to refer to the CTE again.
	 *
	 * @see JpaCriteriaQuery#from(JpaCteCriteria)
	 */
	<T> JpaCteCriteria<T> withRecursiveUnionAll(AbstractQuery<T> baseCriteria, Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);

	/**
	 * Allows to register a recursive CTE. The base {@link CriteriaQuery} serves
	 * for the structure of the {@link JpaCteCriteria}, which is made available in the recursive criteria producer function,
	 * so that the recursive {@link CriteriaQuery} is able to refer to the CTE again.
	 *
	 * @see JpaCriteriaQuery#from(JpaCteCriteria)
	 */
	<T> JpaCteCriteria<T> withRecursiveUnionDistinct(AbstractQuery<T> baseCriteria, Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);

	/**
	 * Like {@link #with(AbstractQuery)} but assigns an explicit CTE name.
	 */
	<T> JpaCteCriteria<T> with(String name, AbstractQuery<T> criteria);

	/**
	 * Like {@link #withRecursiveUnionAll(AbstractQuery, Function)} but assigns an explicit CTE name.
	 */
	<T> JpaCteCriteria<T> withRecursiveUnionAll(String name, AbstractQuery<T> baseCriteria, Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);

	/**
	 * Like {@link #withRecursiveUnionDistinct(AbstractQuery, Function)} but assigns an explicit CTE name.
	 */
	<T> JpaCteCriteria<T> withRecursiveUnionDistinct(String name, AbstractQuery<T> baseCriteria, Function<JpaCteCriteria<T>, AbstractQuery<T>> recursiveCriteriaProducer);
}
