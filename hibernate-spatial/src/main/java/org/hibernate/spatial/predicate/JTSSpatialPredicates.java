/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.predicate;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.spatial.SpatialFunction;

import org.locationtech.jts.geom.Geometry;

import static org.hibernate.spatial.CommonSpatialFunction.ST_CONTAINS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_CROSSES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_DISJOINT;
import static org.hibernate.spatial.CommonSpatialFunction.ST_EQUALS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_INTERSECTS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_OVERLAPS;
import static org.hibernate.spatial.CommonSpatialFunction.ST_TOUCHES;
import static org.hibernate.spatial.CommonSpatialFunction.ST_WITHIN;

/**
 * A factory for spatial JPA Criteria API {@link Predicate}s.
 *
 * @author Daniel Shuy
 */
public class JTSSpatialPredicates {

	protected JTSSpatialPredicates() {
	}

	/**
	 * Create a predicate for testing the arguments for "spatially equal" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially equal" predicate
	 */
	public static Predicate eq(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_EQUALS.name(), boolean.class, geometry1, geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially equal" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially equal" predicate
	 *
	 * @see #eq(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate eq(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return eq( criteriaBuilder, geometry1, criteriaBuilder.literal( geometry2 ) );
	}

	/**
	 * Create a predicate for testing the arguments for "spatially within" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially within" predicate
	 */
	public static Predicate within(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_WITHIN.name(), boolean.class, geometry1, geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially within" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially within" predicate
	 *
	 * @see #within(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate within(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return within( criteriaBuilder, geometry1, criteriaBuilder.literal( geometry2 ) );
	}

	/**
	 * Create a predicate for testing the arguments for "spatially contains" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially contains" predicate
	 */
	public static Predicate contains(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_CONTAINS.name(), boolean.class, geometry1, geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially contains" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially contains" predicate
	 *
	 * @see #contains(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate contains(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return contains( criteriaBuilder, geometry1,
						 criteriaBuilder.literal( geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially crosses" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially crosses" predicate
	 */
	public static Predicate crosses(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_CROSSES.name(), boolean.class,
										  geometry1, geometry2
				)
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially crosses" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially crosses" predicate
	 *
	 * @see #crosses(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate crosses(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return crosses( criteriaBuilder, geometry1,
						criteriaBuilder.literal( geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially disjoint" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially disjoint" predicate
	 */
	public static Predicate disjoint(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_DISJOINT.name(), boolean.class, geometry1, geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially disjoint" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially disjoint" predicate
	 *
	 * @see #disjoint(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate disjoint(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return disjoint( criteriaBuilder, geometry1,
						 criteriaBuilder.literal( geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially intersects" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially intersects" predicate
	 */
	public static Predicate intersects(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_INTERSECTS.name(), boolean.class, geometry1, geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially intersects" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially intersects" predicate
	 *
	 * @see #intersects(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate intersects(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return intersects( criteriaBuilder, geometry1, criteriaBuilder.literal( geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially overlaps" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially overlaps" predicate
	 */
	public static Predicate overlaps(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_OVERLAPS.name(), boolean.class, geometry1, geometry2
				)
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially overlaps" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially overlaps" predicate
	 *
	 * @see #overlaps(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate overlaps(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return overlaps( criteriaBuilder, geometry1, criteriaBuilder.literal( geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially touches" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 *
	 * @return "spatially touches" predicate
	 */
	public static Predicate touches(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( ST_TOUCHES.name(), boolean.class, geometry1, geometry2 )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "spatially touches" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 *
	 * @return "spatially touches" predicate
	 *
	 * @return "spatially touches" predicate
	 */
	public static Predicate touches(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2) {
		return touches( criteriaBuilder, geometry1, criteriaBuilder.literal( geometry2 ) );
	}



//	/**
//	 * Create a predicate for testing the arguments for bounding box overlap constraint.
//	 *
//	 * @param nodeBuilder NodeBuilder
//	 * @param geometry1 geometry expression
//	 * @param geometry2 geometry expression whose bounding box to use in the comparison
//	 *
//	 * @return bounding box overlap predicate
//	 *
//	 * @see JTSFilterPredicate
//	 */
//	public static Predicate filter(
//			NodeBuilder nodeBuilder, Expression<? extends Geometry> geometry1,
//			Expression<? extends Geometry> geometry2) {
//		return booleanExpressionToPredicate(
//				nodeBuilder,
//				nodeBuilder.function( SpatialFunction.filter.toString(), boolean.class,
//										  geometry1, geometry2
//				)
//		);
//	}

//	/**
//	 * Create a predicate for testing the arguments for bounding box overlap constraint.
//	 *
//	 * @param nodeBuilder NodeBuilder
//	 * @param geometry1 geometry expression
//	 * @param geometry2 geometry value whose bounding box to use in the comparison
//	 *
//	 * @return bounding box overlap predicate
//	 *
//	 * @see JTSFilterPredicate*
//	 */
//	public static Predicate filter(
//			NodeBuilder nodeBuilder, Expression<? extends Geometry> geometry1,
//			Geometry geometry2) {
//		return new JTSFilterPredicate( nodeBuilder, geometry1, geometry2 );
//	}

//	/**
//	 * Create a predicate for testing the arguments for bounding box overlap constraint.
//	 *
//	 * @param nodeBuilder CriteriaBuilder
//	 * @param geometry geometry expression
//	 * @param envelope envelope or bounding box to use in the comparison
//	 * @param srid the SRID of the bounding box
//	 *
//	 * @return bounding box overlap predicate
//	 *
//	 * @see JTSFilterPredicate
//	 */
//	public static Predicate filterByPolygon(
//			NodeBuilder nodeBuilder, Expression<? extends Geometry> geometry,
//			Envelope envelope, int srid) {
//		return new JTSFilterPredicate( nodeBuilder, geometry, envelope, srid );
//	}

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 * @param distance distance expression
	 *
	 * @return "distance within" predicate
	 */
	public static Predicate distanceWithin(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2, Expression<Double> distance) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( SpatialFunction.dwithin.toString(), boolean.class,
										  geometry1, geometry2, distance
				)
		);
	}

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 * @param distance distance expression
	 *
	 * @return "distance within" predicate
	 *
	 * @see #distanceWithin(CriteriaBuilder, Expression, Expression, Expression)
	 */
	public static Predicate distanceWithin(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2, Expression<Double> distance) {
		return distanceWithin( criteriaBuilder, geometry1,
							   criteriaBuilder.literal( geometry2 ), distance
		);
	}

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry value
	 * @param distance distance value
	 *
	 * @return "distance within" predicate
	 *
	 * @see #distanceWithin(CriteriaBuilder, Expression, Expression, Expression)
	 */
	public static Predicate distanceWithin(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Geometry geometry2, double distance) {
		return distanceWithin( criteriaBuilder, geometry1,
							   criteriaBuilder.literal( geometry2 ), criteriaBuilder.literal( distance )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "distance within" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry1 geometry expression
	 * @param geometry2 geometry expression
	 * @param distance distance value
	 *
	 * @return "distance within" predicate
	 *
	 * @see #distanceWithin(CriteriaBuilder, Expression, Expression, Expression)
	 */
	public static Predicate distanceWithin(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry1,
			Expression<? extends Geometry> geometry2, double distance) {
		return distanceWithin( criteriaBuilder, geometry1, geometry2,
							   criteriaBuilder.literal( distance )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "having srid" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry geometry expression
	 * @param srid SRID expression
	 *
	 * @return "having srid" predicate
	 */
	public static Predicate havingSRID(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			Expression<Integer> srid) {
		return criteriaBuilder.equal(
				criteriaBuilder.function( SpatialFunction.srid.toString(), int.class, geometry ),
				srid
		);
	}

	/**
	 * Create a predicate for testing the arguments for "having srid" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry geometry expression
	 * @param srid SRID expression
	 *
	 * @return "having srid" predicate
	 *
	 * @see #havingSRID(CriteriaBuilder, Expression, Expression)
	 */
	public static Predicate havingSRID(
			CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry,
			int srid) {
		return havingSRID( criteriaBuilder, geometry,
						   criteriaBuilder.literal( srid )
		);
	}

	/**
	 * Create a predicate for testing the arguments for "is empty" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry geometry expression
	 *
	 * @return "is empty" predicate
	 */
	public static Predicate isEmpty(CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry) {
		return booleanExpressionToPredicate(
				criteriaBuilder,
				criteriaBuilder.function( SpatialFunction.isempty.toString(), boolean.class,
										  geometry
				)
		);
	}

	/**
	 * Create a predicate for testing the arguments for "is not empty" constraint.
	 *
	 * @param criteriaBuilder CriteriaBuilder
	 * @param geometry geometry expression
	 *
	 * @return "is not empty" predicate
	 */
	public static Predicate isNotEmpty(CriteriaBuilder criteriaBuilder, Expression<? extends Geometry> geometry) {
		return isEmpty( criteriaBuilder, geometry )
				.not();
	}

	private static Predicate booleanExpressionToPredicate(
			CriteriaBuilder criteriaBuilder,
			Expression<Boolean> expression) {
		return criteriaBuilder.equal( expression, true );
	}
}
