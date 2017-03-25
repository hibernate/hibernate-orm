/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * Hibernate extensions to the JPA CriteriaBuilder.  Currently there are no extensions; these are coming in 6.0
 *
 * @author Steve Ebersole
 */
public interface HibernateCriteriaBuilder extends CriteriaBuilder {
	/**
	 * Create a predicate that tests whether a Map is empty.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#isEmpty}
	 *
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for emptiness
	 *
	 * @return is-empty predicate
	 */
	<M extends Map<?,?>> Predicate isMapEmpty(Expression<M> mapExpression);

	/**
	 * Create a predicate that tests whether a Map is
	 * not empty.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#isNotEmpty}
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for non-emptiness
	 *
	 * @return is-not-empty predicate
	 */
	<M extends Map<?,?>> Predicate isMapNotEmpty(Expression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#size}
	 *
	 * @param mapExpression The expression resolving to a Map for which we
	 * want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?,?>> Expression<Integer> mapSize(Expression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @param map The Map for which we want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?,?>> Expression<Integer> mapSize(M map);

}
