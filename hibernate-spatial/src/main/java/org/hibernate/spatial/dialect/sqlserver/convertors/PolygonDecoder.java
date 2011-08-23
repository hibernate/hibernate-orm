/*
 * $Id: PolygonDecoder.java 201 2010-04-05 13:49:25Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
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

package org.hibernate.spatial.dialect.sqlserver.convertors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import org.hibernate.spatial.mgeom.MGeometryFactory;

/**
 * @author Karel Maesen, Geovise BVBA
 */
class PolygonDecoder extends AbstractDecoder<Polygon> {

	public PolygonDecoder(MGeometryFactory factory) {
		super( factory );
	}

	@Override
	protected OpenGisType getOpenGisType() {
		return OpenGisType.POLYGON;
	}

	protected Polygon createNullGeometry() {
		return getGeometryFactory().createPolygon( null, null );
	}

	protected Polygon createGeometry(SqlServerGeometry nativeGeom) {
		return createGeometry( nativeGeom, 0 );
	}

	protected Polygon createGeometry(SqlServerGeometry nativeGeom, int shapeIndex) {
		if ( nativeGeom.isEmptyShape( shapeIndex ) ) {
			return createNullGeometry();
		}
		//polygons consist of one exterior ring figure, and several interior ones.
		IndexRange figureRange = nativeGeom.getFiguresForShape( shapeIndex );
		LinearRing[] holes = new LinearRing[figureRange.length() - 1];
		LinearRing shell = null;
		for ( int figureIdx = figureRange.start, i = 0; figureIdx < figureRange.end; figureIdx++ ) {
			IndexRange pntIndexRange = nativeGeom.getPointsForFigure( figureIdx );
			if ( nativeGeom.isFigureInteriorRing( figureIdx ) ) {
				holes[i++] = toLinearRing( nativeGeom, pntIndexRange );
			}
			else {
				shell = toLinearRing( nativeGeom, pntIndexRange );
			}
		}
		return getGeometryFactory().createPolygon( shell, holes );
	}

	private LinearRing toLinearRing(SqlServerGeometry nativeGeom, IndexRange range) {
		Coordinate[] coordinates = nativeGeom.coordinateRange( range );
		return getGeometryFactory().createLinearRing( coordinates );
	}

}
