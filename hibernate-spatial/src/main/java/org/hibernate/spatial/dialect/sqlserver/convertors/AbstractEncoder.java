/*
 * $Id: AbstractEncoder.java 162 2010-03-07 21:21:38Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2009 Geovise BVBA
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

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.spatial.mgeom.MGeometry;


abstract class AbstractEncoder<G extends Geometry> implements Encoder<G> {

	public SqlServerGeometry encode(G geom) {
		SqlServerGeometry nativeGeom = new SqlServerGeometry();
		nativeGeom.setSrid( geom.getSRID() );
		if ( geom.isValid() ) {
			nativeGeom.setIsValid();
		}

		if ( hasMValues( geom ) ) {
			nativeGeom.setHasMValues();
		}

		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		List<Figure> figures = new ArrayList<Figure>();
		List<Shape> shapes = new ArrayList<Shape>();

		encode( geom, -1, coordinates, figures, shapes );
		encodePoints( nativeGeom, coordinates );
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
	protected abstract void encode(Geometry geom, int parentShapeIndex, List<Coordinate> coordinates, List<Figure> figures, List<Shape> shapes);

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

	protected boolean hasMValues(G geom) {
		return geom instanceof MGeometry;
	}


	protected void encodePoints(SqlServerGeometry nativeGeom, List<Coordinate> coordinates) {
		nativeGeom.setNumberOfPoints( coordinates.size() );
		nativeGeom.allocateMValueArray();
		for ( int i = 0; i < coordinates.size(); i++ ) {
			setCoordinate( nativeGeom, i, coordinates.get( i ) );
		}
	}

	protected void setCoordinate(SqlServerGeometry nativeGeom, int idx, Coordinate coordinate) {
		if ( !nativeGeom.hasZValues() && !Double.isNaN( coordinate.z ) ) {
			nativeGeom.setHasZValues();
			nativeGeom.allocateZValueArray();
		}

		nativeGeom.setCoordinate( idx, coordinate );
	}

}
