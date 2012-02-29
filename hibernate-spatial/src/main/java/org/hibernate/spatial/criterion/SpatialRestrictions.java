/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.hibernate.spatial.criterion;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.criterion.Criterion;
import org.hibernate.spatial.SpatialRelation;

/**
 * Static Factory Class for creating spatial criterion types.
 * <p/>
 * <p>
 * The criterion types created by this class implement the spatial query
 * expressions of the OpenGIS Simple Features Specification for SQL, Revision
 * 1.1.
 * <p/>
 * In addition, it provides for a simple spatial <code>filter</code> that
 * works mostly using the spatial index. This corresponds to the Oracle
 * Spatial's "SDO_FILTER" function, or the "&&" operator of PostGIS.
 * </p>
 *
 * @author Karel Maesen
 */
public class SpatialRestrictions {

	SpatialRestrictions() {
	}

	public static SpatialRelateExpression eq(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.EQUALS
		);
	}


	public static SpatialRelateExpression within(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.WITHIN
		);
	}

	public static SpatialRelateExpression contains(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.CONTAINS
		);
	}

	public static SpatialRelateExpression crosses(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.CROSSES
		);
	}

	public static SpatialRelateExpression disjoint(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.DISJOINT
		);
	}

	public static SpatialRelateExpression intersects(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.INTERSECTS
		);
	}

	public static SpatialRelateExpression overlaps(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.OVERLAPS
		);
	}

	public static SpatialRelateExpression touches(String propertyName, Geometry value) {
		return new SpatialRelateExpression(
				propertyName, value,
				SpatialRelation.TOUCHES
		);
	}

	public static SpatialFilter filter(String propertyName, Geometry filter) {
		return new SpatialFilter(propertyName, filter);
	}

	public static SpatialFilter filter(String propertyName, Envelope envelope,
									   int SRID) {
		return new SpatialFilter(propertyName, envelope, SRID);
	}

	public static Criterion distanceWithin(String propertyName, Geometry geometry, double distance) {
		return new DWithinExpression(propertyName, geometry, distance);
	}


	public static Criterion havingSRID(String propertyName, int srid) {
		return new HavingSridExpression(propertyName, srid);
	}

	public static Criterion isEmpty(String propertyName) {
		return new IsEmptyExpression(propertyName, true);
	}

	public static Criterion isNotEmpty(String propertyName) {
		return new IsEmptyExpression(propertyName, false);
	}

	public static Criterion spatialRestriction(int relation,
											   String propertyName, Geometry value) {
		switch (relation) {
			case SpatialRelation.CONTAINS:
				return contains(propertyName, value);
			case SpatialRelation.CROSSES:
				return crosses(propertyName, value);
			case SpatialRelation.DISJOINT:
				return disjoint(propertyName, value);
			case SpatialRelation.INTERSECTS:
				return intersects(propertyName, value);
			case SpatialRelation.EQUALS:
				return eq(propertyName, value);
			case SpatialRelation.FILTER:
				return filter(propertyName, value);
			case SpatialRelation.OVERLAPS:
				return overlaps(propertyName, value);
			case SpatialRelation.TOUCHES:
				return touches(propertyName, value);
			case SpatialRelation.WITHIN:
				return within(propertyName, value);
			default:
				throw new IllegalArgumentException(
						"Non-existant spatial relation passed."
				);
		}
	}
}
