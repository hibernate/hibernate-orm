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
package org.hibernate.spatial.jts.mgeom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;

import java.io.Serializable;

/**
 * Creates MCoordinateSequenceFactory internally represented as an array of
 * {@link MCoordinate}s.
 */
public class MCoordinateSequenceFactory implements CoordinateSequenceFactory,
		Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static MCoordinateSequenceFactory instance = new MCoordinateSequenceFactory();

	private MCoordinateSequenceFactory() {
	}

	/**
	 * Returns the singleton instance of MCoordinateSequenceFactory
	 */
	public static MCoordinateSequenceFactory instance() {
		return instance;
	}

	/**
	 * Returns an MCoordinateSequence based on the given array -- the array is
	 * used directly if it is an instance of MCoordinate[]; otherwise it is
	 * copied.
	 */
	public CoordinateSequence create(Coordinate[] coordinates) {
		return coordinates instanceof MCoordinate[] ? new MCoordinateSequence(
				(MCoordinate[]) coordinates
		) : new MCoordinateSequence(
				coordinates
		);
	}

	public CoordinateSequence create(CoordinateSequence coordSeq) {
		return new MCoordinateSequence(coordSeq);
	}

	/**
	 * Creates a MCoordinateSequence instance initialized to the size parameter.
	 * Note that the dimension argument is ignored.
	 *
	 * @see com.vividsolutions.jts.geom.CoordinateSequenceFactory#create(int, int)
	 */
	public CoordinateSequence create(int size, int dimension) {
		return new MCoordinateSequence(size);
	}

}
