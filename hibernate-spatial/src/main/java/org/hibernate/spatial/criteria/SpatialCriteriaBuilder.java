/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.criteria;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Marco Belladelli
 */
public interface SpatialCriteriaBuilder<T> extends HibernateCriteriaBuilder {

	Predicate eq(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate eq(Expression<? extends T> geometry1, T geometry2);

	Predicate within(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate within(Expression<? extends T> geometry1, T geometry2);

	Predicate contains(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate contains(Expression<? extends T> geometry1, T geometry2);

	Predicate crosses(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate crosses(Expression<? extends T> geometry1, T  geometry2);

	Predicate disjoint(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate disjoint(Expression<? extends T> geometry1, T  geometry2);

	Predicate intersects(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate intersects(Expression<? extends T> geometry1, T  geometry2);

	Predicate overlaps(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate overlaps(Expression<? extends T> geometry1, T  geometry2);

	Predicate touches(Expression<? extends T> geometry1, Expression<? extends T> geometry2);

	Predicate touches(Expression<? extends T> geometry1, T  geometry2);

	Predicate distanceWithin(
			Expression<? extends T> geometry1,
			Expression<? extends T> geometry2,
			Expression<Double> distance);

	Predicate distanceWithin(Expression<? extends T> geometry1, T  geometry2, Expression<Double> distance);

	Predicate distanceWithin(Expression<? extends T> geometry1, T  geometry2, double distance);

	Predicate distanceWithin(
			Expression<? extends T> geometry1,
			Expression<? extends T> geometry2,
			double distance);

	Predicate havingSRID(Expression<? extends T> geometry, Expression<Integer> srid);

	Predicate havingSRID(Expression<? extends T> geometry, int srid);

	Predicate isEmpty(CriteriaBuilder criteriaBuilder, Expression<? extends T> geometry);

	Predicate isNotEmpty(CriteriaBuilder criteriaBuilder, Expression<? extends T> geometry);
}
