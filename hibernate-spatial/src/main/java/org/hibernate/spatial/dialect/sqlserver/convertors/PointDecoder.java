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


import org.geolatte.geom.DimensionalFlag;
import org.geolatte.geom.Point;
import org.geolatte.geom.PointSequence;
import org.geolatte.geom.crs.CrsId;

/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
class PointDecoder extends AbstractDecoder<Point> {


	@Override
	protected OpenGisType getOpenGisType() {
		return OpenGisType.POINT;
	}

	protected Point createNullGeometry() {
		return Point.createEmpty();
	}

	protected Point createGeometry(SqlServerGeometry nativeGeom) {
		return createPoint( nativeGeom, 0 );
	}

	@Override
	protected Point createGeometry(SqlServerGeometry nativeGeom, int shapeIndex) {
		if ( nativeGeom.isEmptyShape( shapeIndex ) ) {
			return createNullGeometry();
		}
		int figureOffset = nativeGeom.getFiguresForShape( shapeIndex ).start;
		int pntOffset = nativeGeom.getPointsForFigure( figureOffset ).start;
		return createPoint( nativeGeom, pntOffset );
	}

	private Point createPoint(SqlServerGeometry nativeGeom, int pntOffset) {
        DimensionalFlag df = DimensionalFlag.valueOf(nativeGeom.hasZValues(), nativeGeom.hasMValues());
        PointSequence pointSequence = nativeGeom.coordinateRange(new IndexRange(pntOffset, pntOffset + 1));
        return new Point(pointSequence, CrsId.valueOf(nativeGeom.getSrid()));
    }


}
