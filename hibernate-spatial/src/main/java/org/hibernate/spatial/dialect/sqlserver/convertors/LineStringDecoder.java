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

import org.geolatte.geom.LineString;
import org.geolatte.geom.PointSequence;
import org.geolatte.geom.crs.CrsId;

class LineStringDecoder extends AbstractDecoder<LineString> {


	@Override
	protected OpenGisType getOpenGisType() {
		return OpenGisType.LINESTRING;
	}

	protected LineString createNullGeometry() {
		return LineString.createEmpty();
	}

	protected LineString createGeometry(SqlServerGeometry nativeGeom) {
		return createLineString( nativeGeom, new IndexRange( 0, nativeGeom.getNumPoints() ) );
	}

	@Override
	protected LineString createGeometry(SqlServerGeometry nativeGeom, int shapeIndex) {
		if ( nativeGeom.isEmptyShape( shapeIndex ) ) {
			return createNullGeometry();
		}
		int figureOffset = nativeGeom.getFiguresForShape( shapeIndex ).start;
		IndexRange pntIndexRange = nativeGeom.getPointsForFigure( figureOffset );
		return createLineString( nativeGeom, pntIndexRange );
	}

	protected LineString createLineString(SqlServerGeometry nativeGeom, IndexRange pntIndexRange) {
		PointSequence coordinates = nativeGeom.coordinateRange( pntIndexRange );
		return new LineString(coordinates, CrsId.valueOf(nativeGeom.getSrid()));
	}
}
