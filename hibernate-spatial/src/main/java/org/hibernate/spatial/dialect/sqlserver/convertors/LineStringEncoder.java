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

import org.geolatte.geom.Geometry;
import org.geolatte.geom.LineString;
import org.geolatte.geom.Point;
import org.geolatte.geom.PointCollection;
import org.geolatte.geom.PointSequence;


class LineStringEncoder extends AbstractEncoder<LineString> {

	@Override
	protected void encode(Geometry geom, int parentShapeIndex, CountingPointSequenceBuilder coordinates, List<Figure> figures, List<Shape> shapes) {
		if ( !( geom instanceof LineString ) ) {
			throw new IllegalArgumentException( "Require LineString geometry" );
		}
		if ( geom.isEmpty() ) {
			shapes.add( new Shape( parentShapeIndex, -1, OpenGisType.LINESTRING ) );
			return;
		}
		int figureOffset = figures.size();
        int pointOffset = coordinates.getNumAdded();
		for ( Point point : (PointSequence)geom.getPoints() ) {
			coordinates.add( point );
		}
		figures.add( new Figure( FigureAttribute.Stroke, pointOffset ) );
		shapes.add( new Shape( parentShapeIndex, figureOffset, OpenGisType.LINESTRING ) );
	}

	@Override
	protected void encodePoints(SqlServerGeometry nativeGeom, PointCollection coordinates) {
		super.encodePoints( nativeGeom, coordinates );
		if ( coordinates.size() == 2 ) {
			nativeGeom.setIsSingleLineSegment();
		}
	}

	public boolean accepts(Geometry geom) {
		return geom instanceof LineString;
	}
}
