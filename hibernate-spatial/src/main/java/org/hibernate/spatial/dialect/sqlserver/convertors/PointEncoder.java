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
import org.geolatte.geom.Point;


/**
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
class PointEncoder extends AbstractEncoder<Point> {

	/**
	 * Encodes a point as an <code>SQLGeometryV1</code> object.
	 * <p/>
	 * This is a specific implementation because points don't explicitly serialize figure and shape components.
	 *
	 * @param geom Geometry to serialize
	 *
	 * @return
	 */
	@Override
	public SqlServerGeometry encode(Point geom) {

		SqlServerGeometry sqlServerGeom = new SqlServerGeometry();
		int srid = geom.getSRID();
		sqlServerGeom.setSrid( srid < 0 ? 0 : srid );
		sqlServerGeom.setIsValid();

		if ( geom.isEmpty() ) {
			sqlServerGeom.setNumberOfPoints( 0 );
			sqlServerGeom.setNumberOfFigures( 0 );
			sqlServerGeom.setNumberOfShapes( 1 );
			sqlServerGeom.setShape( 0, new Shape( -1, -1, OpenGisType.POINT ) );
			return sqlServerGeom;
		}

		sqlServerGeom.setIsSinglePoint();
		sqlServerGeom.setNumberOfPoints( 1 );
		if ( geom.is3D() ) {
			sqlServerGeom.setHasZValues();
			sqlServerGeom.allocateZValueArray();
		}
		if ( geom.isMeasured() ) {
			sqlServerGeom.setHasMValues();
			sqlServerGeom.allocateMValueArray();
		}
		sqlServerGeom.setCoordinate( 0, geom.getPoints() );
		return sqlServerGeom;
	}

    @Override
    protected void encode(Geometry geom, int parentIdx, CountingPointSequenceBuilder coordinates, List<Figure> figures, List<Shape> shapes) {
		if ( !( geom instanceof Point ) ) {
			throw new IllegalArgumentException( "Require Point geometry" );
		}
		if ( geom.isEmpty() ) {
			shapes.add( new Shape( parentIdx, -1, OpenGisType.POINT ) );
			return;
		}
		int pntOffset = coordinates.getNumAdded();
		int figureOffset = figures.size();
		coordinates.add( geom.getPointN(0) );
		Figure figure = new Figure( FigureAttribute.Stroke, pntOffset );
		figures.add( figure );
		Shape shape = new Shape( parentIdx, figureOffset, OpenGisType.POINT );
		shapes.add( shape );
	}

	public boolean accepts(Geometry geom) {
		return geom instanceof Point;
	}
}
