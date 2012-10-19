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

import java.util.ArrayList;
import java.util.List;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.PointCollection;


abstract class AbstractEncoder<G extends Geometry> implements Encoder<G> {

	public SqlServerGeometry encode(G geom) {
		SqlServerGeometry nativeGeom = new SqlServerGeometry();
		int srid = geom.getSRID();
		nativeGeom.setSrid( srid < 0 ? 0 : srid );
		nativeGeom.setIsValid();

		if ( geom.isMeasured() ) {
			nativeGeom.setHasMValues();
		}

        CountingPointSequenceBuilder coordinates = new CountingPointSequenceBuilder(geom.getDimensionalFlag());
		List<Figure> figures = new ArrayList<Figure>();
		List<Shape> shapes = new ArrayList<Shape>();

		encode( geom, -1, coordinates, figures, shapes );
		encodePoints( nativeGeom, coordinates.toPointSequence() );
		encodeFigures( nativeGeom, figures );
		encodeShapes( nativeGeom, shapes );
		return nativeGeom;
	}

	/**
	 * Appends the points, figures, shapes to the resp. lists
	 *
	 * @param geom geometry to serialization
	 * @param parentShapeIndex index of the parent Shape for the geometry
	 * @param coordinates coordinate list to append to
	 * @param figures figure list to append to
	 * @param shapes shape list to append to
	 */
	protected abstract void encode(Geometry geom, int parentShapeIndex, CountingPointSequenceBuilder coordinates, List<Figure> figures, List<Shape> shapes);

	protected void encodeShapes(SqlServerGeometry nativeGeom, List<Shape> shapes) {
		nativeGeom.setNumberOfShapes( shapes.size() );
		for ( int i = 0; i < shapes.size(); i++ ) {
			nativeGeom.setShape( i, shapes.get( i ) );
		}
	}

	protected void encodeFigures(SqlServerGeometry nativeGeom, List<Figure> figures) {
		nativeGeom.setNumberOfFigures( figures.size() );
		for ( int i = 0; i < figures.size(); i++ ) {
			nativeGeom.setFigure( i, figures.get( i ) );
		}
	}


	protected void encodePoints(SqlServerGeometry nativeGeom, PointCollection coordinates) {
		nativeGeom.setNumberOfPoints( coordinates.size() );
		nativeGeom.allocateMValueArray();
		for ( int i = 0; i < coordinates.size(); i++ ) {
			setCoordinate( nativeGeom, i, coordinates);
		}
	}

	protected void setCoordinate(SqlServerGeometry nativeGeom, int idx, PointCollection coordinate) {
		if ( !nativeGeom.hasZValues() && !Double.isNaN( coordinate.getZ(idx) ) ) {
			nativeGeom.setHasZValues();
			nativeGeom.allocateZValueArray();
		}

		nativeGeom.setCoordinate( idx, coordinate );
	}

}
