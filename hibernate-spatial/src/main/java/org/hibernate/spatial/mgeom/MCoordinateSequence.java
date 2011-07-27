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
import com.vividsolutions.jts.geom.Envelope;

/**
 * Implements the CoordinateSequence interface. In this implementation,
 * Coordinates returned by #toArray and #get are live -- parties that change
 * them are actually changing the MCoordinateSequence's underlying data.
 */
public class MCoordinateSequence implements CoordinateSequence, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private MCoordinate[] coordinates;

	public static MCoordinate[] copy(Coordinate[] coordinates) {
		MCoordinate[] copy = new MCoordinate[coordinates.length];
		for ( int i = 0; i < coordinates.length; i++ ) {
			copy[i] = new MCoordinate( coordinates[i] );
		}
		return copy;
	}

	public static MCoordinate[] copy(CoordinateSequence coordSeq) {
		MCoordinate[] copy = new MCoordinate[coordSeq.size()];
		for ( int i = 0; i < coordSeq.size(); i++ ) {
			copy[i] = new MCoordinate( coordSeq.getCoordinate( i ) );
		}
		return copy;
	}

	/**
	 * Copy constructor -- simply aliases the input array, for better
	 * performance.
	 *
	 * @param coordinates
	 */
	public MCoordinateSequence(MCoordinate[] coordinates) {
		this.coordinates = coordinates;
	}

	/**
	 * Constructor that makes a copy of an array of Coordinates. Always makes a
	 * copy of the input array, since the actual class of the Coordinates in the
	 * input array may be different from MCoordinate.
	 *
	 * @param copyCoords
	 */
	public MCoordinateSequence(Coordinate[] copyCoords) {
		coordinates = copy( copyCoords );
	}

	/**
	 * Constructor that makes a copy of a CoordinateSequence.
	 *
	 * @param coordSeq
	 */
	public MCoordinateSequence(CoordinateSequence coordSeq) {
		coordinates = copy( coordSeq );
	}

	/**
	 * Constructs a sequence of a given size, populated with new
	 * {@link MCoordinate}s.
	 *
	 * @param size the size of the sequence to create
	 */
	public MCoordinateSequence(int size) {
		coordinates = new MCoordinate[size];
		for ( int i = 0; i < size; i++ ) {
			coordinates[i] = new MCoordinate();
		}
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#getDimension()
	 */
	public int getDimension() {
		return 4;
	}

	public Coordinate getCoordinate(int i) {
		return coordinates[i];
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#getCoordinateCopy(int)
	 */
	public Coordinate getCoordinateCopy(int index) {
		return new Coordinate( coordinates[index] );
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#getCoordinate(int,
	 *	  com.vividsolutions.jts.geom.Coordinate)
	 */
	public void getCoordinate(int index, Coordinate coord) {
		coord.x = coordinates[index].x;
		coord.y = coordinates[index].y;
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#getX(int)
	 */
	public double getX(int index) {
		return coordinates[index].x;
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#getY(int)
	 */
	public double getY(int index) {
		return coordinates[index].y;
	}

	/**
	 * @return the measure value of the coordinate in the index
	 */
	public double getM(int index) {
		return coordinates[index].m;
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#getOrdinate(int, int)
	 */
	public double getOrdinate(int index, int ordinateIndex) {
		switch ( ordinateIndex ) {
			case CoordinateSequence.X:
				return coordinates[index].x;
			case CoordinateSequence.Y:
				return coordinates[index].y;
			case CoordinateSequence.Z:
				return coordinates[index].z;
			case CoordinateSequence.M:
				return coordinates[index].m;
		}
		return Double.NaN;
	}

	/**
	 * @see com.vividsolutions.jts.geom.CoordinateSequence#setOrdinate(int, int, double)
	 */
	public void setOrdinate(int index, int ordinateIndex, double value) {
		switch ( ordinateIndex ) {
			case CoordinateSequence.X:
				coordinates[index].x = value;
				break;
			case CoordinateSequence.Y:
				coordinates[index].y = value;
				break;
			case CoordinateSequence.Z:
				coordinates[index].z = value;
				break;
			case CoordinateSequence.M:
				coordinates[index].m = value;
				break;
			default:
				throw new IllegalArgumentException( "invalid ordinateIndex" );
		}
	}

	public Object clone() {
		MCoordinate[] cloneCoordinates = new MCoordinate[size()];
		for ( int i = 0; i < coordinates.length; i++ ) {
			cloneCoordinates[i] = (MCoordinate) coordinates[i].clone();
		}

		return new MCoordinateSequence( cloneCoordinates );
	}

	public int size() {
		return coordinates.length;
	}

	public Coordinate[] toCoordinateArray() {
		return coordinates;
	}

	public Envelope expandEnvelope(Envelope env) {
		for ( int i = 0; i < coordinates.length; i++ ) {
			env.expandToInclude( coordinates[i] );
		}
		return env;
	}

	public String toString() {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append( "MCoordinateSequence [" );
		for ( int i = 0; i < coordinates.length; i++ ) {
			if ( i > 0 ) {
				strBuf.append( ", " );
			}
			strBuf.append( coordinates[i] );
		}
		strBuf.append( "]" );
		return strBuf.toString();
	}
}
