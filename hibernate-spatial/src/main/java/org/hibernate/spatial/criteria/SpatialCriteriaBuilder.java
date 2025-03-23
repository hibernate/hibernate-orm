/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.criteria;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * An extended {@link HibernateCriteriaBuilder} with spatial functionality
 *
 * @author Marco Belladelli
 */
public interface SpatialCriteriaBuilder<T> extends HibernateCriteriaBuilder {

	/**
	 * Create a predicate for testing the arguments for "spatially equal" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially equal" predicate
	 */
	Predicate eq(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially equal" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially equal" predicate
	 *
	 * @see #eq(Expression, Expression)
	 */
	Predicate eq(Expression<? extends T> geometry1, T geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially within" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially within" predicate
	 */
	Predicate within(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially within" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially within" predicate
	 *
	 * @see #within(Expression, Expression)
	 */
	Predicate within(Expression<? extends T> geometry1, T geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially contains" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially contains" predicate
	 */
	Predicate contains(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially contains" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially contains" predicate
	 *
	 * @see #contains(Expression, Expression)
	 */
	Predicate contains(Expression<? extends T> geometry1, T geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially crosses" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially crosses" predicate
	 */
	Predicate crosses(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially crosses" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially crosses" predicate
	 *
	 * @see #crosses(Expression, Expression)
	 */
	Predicate crosses(Expression<? extends T> geometry1, T  geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially disjoint" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially disjoint" predicate
	 */
	Predicate disjoint(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially disjoint" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially disjoint" predicate
	 *
	 * @see #disjoint(Expression, Expression)
	 */
	Predicate disjoint(Expression<? extends T> geometry1, T  geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially intersects" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially intersects" predicate
	 */
	Predicate intersects(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially intersects" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially intersects" predicate
	 *
	 * @see #intersects(Expression, Expression)
	 */
	Predicate intersects(Expression<? extends T> geometry1, T  geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially overlaps" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially overlaps" predicate
	 */
	Predicate overlaps(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially overlaps" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially overlaps" predicate
	 *
	 * @see #overlaps(Expression, Expression)
	 */
	Predicate overlaps(Expression<? extends T> geometry1, T  geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially touches" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially touches" predicate
	 */
	Predicate touches(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	/**
	 * Create a predicate for testing the arguments for "spatially touches" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially touches" predicate
	 *
	 * @see #touches(Expression, Expression)
	 */
	Predicate touches(Expression<? extends T> geometry1, T  geometry2);

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 * @param distance distance expression
	 *
	 * @return "distance within" predicate
	 */
	Predicate distanceWithin(
			Expression<? extends T> geometry1,
			Expression<? extends T> geometry2,
			Expression<Double> distance);

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 * @param distance distance expression
	 *
	 * @return "distance within" predicate
	 *
	 * @see #distanceWithin(Expression, Expression, Expression)
	 */
	Predicate distanceWithin(Expression<? extends T> geometry1, T  geometry2, Expression<Double> distance);

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 * @param distance distance value
	 *
	 * @return "distance within" predicate
	 *
	 * @see #distanceWithin(Expression, Expression, Expression)
	 */
	Predicate distanceWithin(Expression<? extends T> geometry1, T  geometry2, double distance);

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 * @param distance distance value
	 *
	 * @return "distance within" predicate
	 *
	 * @see #distanceWithin(Expression, Expression, Expression)
	 */
	Predicate distanceWithin(
			Expression<? extends T> geometry1,
			Expression<? extends T> geometry2,
			double distance);

	/**
	 * Create a predicate for testing the arguments for "having srid" constraint.
	 *
	 * @param geometry geometry expression
	 * @param srid SRID expression
	 *
	 * @return "having srid" predicate
	 */
	Predicate havingSRID(Expression<? extends T> geometry, Expression<Integer> srid);

	/**
	 * Create a predicate for testing the arguments for "having srid" constraint.
	 *
	 * @param geometry geometry expression
	 * @param srid SRID expression
	 *
	 * @return "having srid" predicate
	 *
	 * @see #havingSRID(Expression, Expression)
	 */
	Predicate havingSRID(Expression<? extends T> geometry, int srid);

	/**
	 * Create a predicate for testing the arguments for "is empty" constraint.
	 *
	 * @param geometry geometry expression
	 *
	 * @return "is empty" predicate
	 */
	Predicate isGeometryEmpty(Expression<? extends T> geometry);

	/**
	 * Create a predicate for testing the arguments for "is not empty" constraint.
	 *
	 * @param geometry geometry expression
	 *
	 * @return "is not empty" predicate
	 */
	Predicate isGeometryNotEmpty(Expression<? extends T> geometry);
}
