/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.criteria.internal;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.spi.HibernateCriteriaBuilderDelegate;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.criteria.SpatialCriteriaBuilder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import static org.hibernate.spatial.CommonSpatialFunction.ST_CONTAINS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CROSSES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DISJOINT;
import static org.hibernate.spatial.CommonSpatialFunction.ST_EQUALS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_INTERSECTS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_OVERLAPS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_TOUCHES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_WITHIN;

/**
 * @author Marco Belladelli
 */
public abstract class SpatialCriteriaBuilderImpl<T> extends HibernateCriteriaBuilderDelegate
		implements SpatialCriteriaBuilder<T> {

	protected SpatialCriteriaBuilderImpl(HibernateCriteriaBuilder criteriaBuilder) {
		super( criteriaBuilder );
	}

	@Override
	public Predicate eq(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_EQUALS.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate eq(Expression<? extends T> geometry1, T geometry2) {
		return eq( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate within(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_WITHIN.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate within(Expression<? extends T> geometry1, T geometry2) {
		return within( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate contains(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_CONTAINS.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate contains(Expression<? extends T> geometry1, T geometry2) {
		return contains( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate crosses(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_CROSSES.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate crosses(Expression<? extends T> geometry1, T geometry2) {
		return crosses( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate disjoint(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_DISJOINT.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate disjoint(Expression<? extends T> geometry1, T geometry2) {
		return disjoint( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate intersects(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_INTERSECTS.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate intersects(Expression<? extends T> geometry1, T geometry2) {
		return intersects( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate overlaps(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_OVERLAPS.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate overlaps(Expression<? extends T> geometry1, T geometry2) {
		return overlaps( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate touches(Expression<? extends T> geometry1, Expression<? extends T> geometry2) {
		return isTrue( function( ST_TOUCHES.name(), boolean.class, geometry1, geometry2 ) );
	}

	@Override
	public Predicate touches(Expression<? extends T> geometry1, T geometry2) {
		return touches( geometry1, value( geometry2 ) );
	}

	@Override
	public Predicate distanceWithin(
			Expression<? extends T> geometry1,
			Expression<? extends T> geometry2,
			Expression<Double> distance) {
		return isTrue( function( SpatialFunction.dwithin.toString(), boolean.class, geometry1, geometry2, distance ) );
	}

	@Override
	public Predicate distanceWithin(Expression<? extends T> geometry1, T geometry2, Expression<Double> distance) {
		return distanceWithin( geometry1, value( geometry2 ), distance );
	}

	@Override
	public Predicate distanceWithin(Expression<? extends T> geometry1, T geometry2, double distance) {
		return distanceWithin( geometry1, value( geometry2 ), value( distance ) );
	}

	@Override
	public Predicate distanceWithin(
			Expression<? extends T> geometry1,
			Expression<? extends T> geometry2,
			double distance) {
		return distanceWithin( geometry1, geometry2, value( distance ) );
	}

	@Override
	public Predicate havingSRID(Expression<? extends T> geometry, Expression<Integer> srid) {
		return equal( function( SpatialFunction.srid.toString(), int.class, geometry ), srid );
	}

	@Override
	public Predicate havingSRID(Expression<? extends T> geometry, int srid) {
		return havingSRID( geometry, value( srid ) );
	}

	@Override
	public Predicate isEmpty(CriteriaBuilder criteriaBuilder, Expression<? extends T> geometry) {
		return isTrue( function( SpatialFunction.isempty.toString(), boolean.class, geometry ) );
	}

	@Override
	public Predicate isNotEmpty(CriteriaBuilder criteriaBuilder, Expression<? extends T> geometry) {
		return isEmpty( criteriaBuilder, geometry ).not();
	}
}
