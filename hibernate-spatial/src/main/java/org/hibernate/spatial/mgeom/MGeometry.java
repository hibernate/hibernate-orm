/**
 * $Id$
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.mgeom;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Defines geometries that carry measures in their CoordinateSequence.
 *
 * @author Karel Maesen
 */

public interface MGeometry extends Cloneable, Serializable {

	/**
	 * Measures are increasing in the direction of the MGeometry
	 */
	public final static int INCREASING = 1;

	/**
	 * Measures are constant across the Geometry
	 */
	public final static int CONSTANT = 0;

	/**
	 * Measures are decreasing in the direction of the MGeometry
	 */
	public final static int DECREASING = -1;

	/**
	 * Measures are not monotone along the Geometry
	 */
	public final static int NON_MONOTONE = -3;

	/**
	 * Returns the measure value at the Coordinate
	 *
	 * @param c the Coordinate for which the measure value is sought
	 * @param tolerance distance to the MGeometry within which Coordinate c has to lie
	 *
	 * @return the measure value if Coordinate c is within tolerance of the
	 *         Geometry, else Double.NaN
	 *         <p>
	 *         When the geometry is a ring or is self-intersecting more
	 *         coordinates may be determined by one coordinate. In that case,
	 *         the lowest measure is returned.
	 *
	 * @throws MGeometryException when this MGeometry is not monotone
	 */
	public double getMatCoordinate(Coordinate c, double tolerance)
			throws MGeometryException;

	/**
	 * Builds measures along the Geometry based on the length from the beginning
	 * (first coordinate) of the Geometry.
	 *
	 * @param keepBeginMeasure -
	 * if true, the measure of the first coordinate is maintained and
	 * used as start value, unless this measure is Double.NaN
	 */
	public void measureOnLength(boolean keepBeginMeasure);

	/**
	 * Returns the Coordinate along the Geometry at the measure value
	 *
	 * @param m measure value
	 *
	 * @return the Coordinate if m is on the MGeometry otherwise null
	 *
	 * @throws MGeometryException when MGeometry is not monotone
	 */
	public Coordinate getCoordinateAtM(double m) throws MGeometryException;

	/**
	 * Returns the coordinatesequence(s) containing all coordinates between the
	 * begin and end measures.
	 *
	 * @param begin begin measure
	 * @param end end measure
	 *
	 * @return an array containing all coordinatesequences in order between
	 *         begin and end. Each CoordinateSequence covers a contiguous
	 *         stretch of the MGeometry.
	 *
	 * @throws MGeometryException when this MGeometry is not monotone
	 */
	public CoordinateSequence[] getCoordinatesBetween(double begin, double end)
			throws MGeometryException;

	/**
	 * Returns the GeometryFactory of the MGeometry
	 *
	 * @return the GeometryFactory of this MGeometry
	 */
	public GeometryFactory getFactory();

	/**
	 * Returns the minimum M-value of the MGeometry
	 *
	 * @return the minimum M-value
	 */
	public double getMinM();

	/**
	 * Returns the maximum M-value of the MGeometry
	 *
	 * @return the maximum M-value
	 */
	public double getMaxM();

	/**
	 * Determine whether the LRS measures (not the x,y,z coordinates) in the
	 * Coordinate sequence of the geometry is Monotone. Monotone implies that
	 * all measures in a sequence of coordinates are consecutively increasing,
	 * decreasing or equal according to the definition of the implementing
	 * geometry. Monotonicity is a pre-condition for most operations on
	 * MGeometries. The following are examples on Monotone measure sequences on
	 * a line string:
	 * <ul>
	 * <li> [0,1,2,3,4] - Monotone Increasing
	 * <li> [4,3,2,1] - Monotone Decreasing
	 * <li> [0,1,1,2,3] - Non-strict Monotone Increasing
	 * <li> [5,3,3,0] - Non-strict Monotone Decreasing
	 * </ul>
	 *
	 * @return true if the coordinates in the CoordinateSequence of the geometry
	 *         are monotone.
	 */
	public boolean isMonotone(boolean strict);

	// /**
	// * Strict Monotone is similar to Monotone, with the added constraint that
	// all measure coordinates
	// * in the CoordinateSequence are ONLY consecutively increasing or
	// decreasing. No consecutive
	// * duplicate measures are allowed.
	// *
	// * @return true if the coordinates in the CoordinateSequence of the
	// geometry are strictly monotone; that is, consitently
	// * increasing or decreasing with no duplicate measures.
	// * @see #isMonotone()
	// */
	// public boolean isStrictMonotone();

	/**
	 * Returns this <code>MGeometry</code> as a <code>Geometry</code>.
	 *
	 * Modifying the returned <code>Geometry</code> will result in internal state changes.
	 *
	 * @return this object as a Geometry.
	 */
	public Geometry asGeometry();

}
