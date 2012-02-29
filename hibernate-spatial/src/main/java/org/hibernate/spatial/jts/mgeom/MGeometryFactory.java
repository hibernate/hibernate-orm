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

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Extension of the GeometryFactory for constructing Geometries with Measure
 * support.
 *
 * @see com.vividsolutions.jts.geom.GeometryFactory
 */
public class MGeometryFactory extends GeometryFactory {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public MGeometryFactory(PrecisionModel precisionModel, int SRID,
							MCoordinateSequenceFactory coordinateSequenceFactory) {
		super( precisionModel, SRID, coordinateSequenceFactory );
	}

	public MGeometryFactory(MCoordinateSequenceFactory coordinateSequenceFactory) {
		super( coordinateSequenceFactory );
	}

	public MGeometryFactory(PrecisionModel precisionModel) {
		this( precisionModel, 0, MCoordinateSequenceFactory.instance() );
	}

	public MGeometryFactory(PrecisionModel precisionModel, int SRID) {
		this( precisionModel, SRID, MCoordinateSequenceFactory.instance() );
	}

	public MGeometryFactory() {
		this( new PrecisionModel(), 0 );
	}

	/**
	 * Constructs a MLineString using the given Coordinates; a null or empty
	 * array will create an empty MLineString.
	 *
	 * @param coordinates array of MCoordinate defining this geometry's vertices
	 *
	 * @return An instance of MLineString containing the coordinates
	 *
	 * @see #createLineString(com.vividsolutions.jts.geom.Coordinate[])
	 */
	public MLineString createMLineString(MCoordinate[] coordinates) {
		return createMLineString(
				coordinates != null ? getCoordinateSequenceFactory()
						.create( coordinates )
						: null
		);
	}

	public MultiMLineString createMultiMLineString(MLineString[] mlines,
												   double mGap) {
		return new MultiMLineString( mlines, mGap, this );
	}

	public MultiMLineString createMultiMLineString(MLineString[] mlines) {
		return new MultiMLineString( mlines, 0.0d, this );
	}

	/**
	 * Creates a MLineString using the given CoordinateSequence; a null or empty
	 * CoordinateSequence will create an empty MLineString.
	 *
	 * @param coordinates a CoordinateSequence possibly empty, or null
	 *
	 * @return An MLineString instance based on the <code>coordinates</code>
	 *
	 * @see #createLineString(com.vividsolutions.jts.geom.CoordinateSequence)
	 */
	public MLineString createMLineString(CoordinateSequence coordinates) {
		return new MLineString( coordinates, this );
	}

}
