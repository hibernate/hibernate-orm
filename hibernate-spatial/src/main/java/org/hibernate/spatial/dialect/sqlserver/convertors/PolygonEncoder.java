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

package org.hibernate.spatial.dialect.sqlserver.convertors;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * <code>Encoder</code> for Polygons.
 *
 * @uthor Karel Maesen, Geovise BVBA
 */
class PolygonEncoder extends AbstractEncoder<Polygon> {

	public boolean accepts(Geometry geom) {
		return geom instanceof Polygon;
	}

	@Override
	protected void encode(Geometry geom, int parentShapeIndex, List<Coordinate> coordinates, List<Figure> figures, List<Shape> shapes) {
		if ( !( geom instanceof Polygon ) ) {
			throw new IllegalArgumentException( "Polygon geometry expected." );
		}
		if ( geom.isEmpty() ) {
			shapes.add( new Shape( parentShapeIndex, -1, OpenGisType.POLYGON ) );
			return;
		}
		Polygon polygon = (Polygon) geom;
		int figureOffset = figures.size();
		shapes.add( new Shape( parentShapeIndex, figureOffset, OpenGisType.POLYGON ) );

		int pointOffset = coordinates.size();
		addExteriorRing( polygon, coordinates, figures );
		addInteriorRings( polygon, coordinates, figures );

	}


	private void addInteriorRings(Polygon geom, List<Coordinate> coordinates, List<Figure> figures) {
		for ( int idx = 0; idx < geom.getNumInteriorRing(); idx++ ) {
			addInteriorRing( geom.getInteriorRingN( idx ), coordinates, figures );
		}
	}

	private void addInteriorRing(LineString ring, List<Coordinate> coordinates, List<Figure> figures) {
		int pointOffset = coordinates.size();
		addPoints( ring, coordinates );
		Figure figure = new Figure( FigureAttribute.InteriorRing, pointOffset );
		figures.add( figure );

	}

	private void addPoints(LineString ring, List<Coordinate> coordinates) {
		for ( Coordinate c : ring.getCoordinates() ) {
			coordinates.add( c );
		}
	}

	private void addExteriorRing(Polygon geom, List<Coordinate> coordinates, List<Figure> figures) {
		LineString shell = geom.getExteriorRing();
		int offset = coordinates.size();
		addPoints( shell, coordinates );
		Figure exterior = new Figure( FigureAttribute.ExteriorRing, offset );
		figures.add( exterior );
	}


}
