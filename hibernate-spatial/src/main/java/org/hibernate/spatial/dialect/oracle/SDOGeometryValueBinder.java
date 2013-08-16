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

package org.hibernate.spatial.dialect.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.hibernate.HibernateException;
import org.hibernate.spatial.helper.FinderException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
class SDOGeometryValueBinder<J> implements ValueBinder<J> {


	private final OracleJDBCTypeFactory typeFactory;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	public SDOGeometryValueBinder(JavaTypeDescriptor<J> javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor, OracleJDBCTypeFactory typeFactory) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.typeFactory = typeFactory;
	}

	@Override
	public void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.STRUCT, SDOGeometry.getTypeName() );
		}
		else {
			final Geometry jtsGeom = javaTypeDescriptor.unwrap( value, Geometry.class, options );
			final Object dbGeom = toNative( jtsGeom, st.getConnection() );
			st.setObject( index, dbGeom );
		}
	}

	public Object store(SDOGeometry geom, Connection conn) throws SQLException, FinderException {
		return typeFactory.createStruct( geom, conn );
	}

	private Object toNative(Geometry jtsGeom, Connection connection) {
		final SDOGeometry geom = convertJTSGeometry( jtsGeom );
		if ( geom != null ) {
			try {
				return store( geom, connection );
			}
			catch ( SQLException e ) {
				throw new HibernateException(
						"Problem during conversion from JTS to SDOGeometry", e
				);
			}
			catch ( FinderException e ) {
				throw new HibernateException(
						"OracleConnection could not be retrieved for creating SDOGeometry STRUCT", e
				);
			}
		}
		else {
			throw new UnsupportedOperationException(
					"Conversion of "
							+ jtsGeom.getClass().getSimpleName()
							+ " to Oracle STRUCT not supported"
			);
		}
	}

	private SDOGeometry convertJTSGeometry(Geometry jtsGeom) {
		SDOGeometry geom = null;
		if ( jtsGeom instanceof Point ) {
			geom = convertJTSPoint( (Point) jtsGeom );
		}
		else if ( jtsGeom instanceof LineString ) {
			geom = convertJTSLineString( (LineString) jtsGeom );
		}
		else if ( jtsGeom instanceof Polygon ) {
			geom = convertJTSPolygon( (Polygon) jtsGeom );
		}
		else if ( jtsGeom instanceof MultiPoint ) {
			geom = convertJTSMultiPoint( (MultiPoint) jtsGeom );
		}
		else if ( jtsGeom instanceof MultiLineString ) {
			geom = convertJTSMultiLineString( (MultiLineString) jtsGeom );
		}
		else if ( jtsGeom instanceof MultiPolygon ) {
			geom = convertJTSMultiPolygon( (MultiPolygon) jtsGeom );
		}
		else if ( jtsGeom instanceof GeometryCollection ) {
			geom = convertJTSGeometryCollection( (GeometryCollection) jtsGeom );
		}
		return geom;
	}

	private SDOGeometry convertJTSGeometryCollection(
			GeometryCollection collection) {
		final SDOGeometry[] sdoElements = new SDOGeometry[collection.getNumGeometries()];
		for ( int i = 0; i < collection.getNumGeometries(); i++ ) {
			final Geometry geom = collection.getGeometryN( i );
			sdoElements[i] = convertJTSGeometry( geom );
		}
		final SDOGeometry ccollect = SDOGeometry.join( sdoElements );
		ccollect.setSRID( collection.getSRID() );
		return ccollect;
	}

	private SDOGeometry convertJTSMultiPolygon(MultiPolygon multiPolygon) {
		final int dim = getCoordDimension( multiPolygon );
		final int lrsPos = getCoordinateLrsPosition( multiPolygon );
		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( new SDOGType( dim, lrsPos, TypeGeometry.MULTIPOLYGON ) );
		geom.setSRID( multiPolygon.getSRID() );
		for ( int i = 0; i < multiPolygon.getNumGeometries(); i++ ) {
			try {
				final Polygon pg = (Polygon) multiPolygon.getGeometryN( i );
				addPolygon( geom, pg );
			}
			catch ( Exception e ) {
				throw new RuntimeException(
						"Found geometry that was not a geometry in MultiPolygon"
				);
			}
		}
		return geom;
	}

	private SDOGeometry convertJTSLineString(LineString lineString) {
		final int dim = getCoordDimension( lineString );
		final int lrsPos = getCoordinateLrsPosition( lineString );
		final boolean isLrs = lrsPos > 0;
		final Double[] ordinates = convertCoordinates( lineString.getCoordinates(), dim, isLrs );
		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( new SDOGType( dim, lrsPos, TypeGeometry.LINE ) );
		geom.setSRID( lineString.getSRID() );
		final ElemInfo info = new ElemInfo( 1 );
		info.setElement( 0, 1, ElementType.LINE_STRAITH_SEGMENTS, 0 );
		geom.setInfo( info );
		geom.setOrdinates( new Ordinates( ordinates ) );
		return geom;

	}

	private SDOGeometry convertJTSMultiPoint(MultiPoint multiPoint) {
		final int dim = getCoordDimension( multiPoint );
		final int lrsDim = getCoordinateLrsPosition( multiPoint );
		final boolean isLrs = ( lrsDim != 0 );
		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( new SDOGType( dim, lrsDim, TypeGeometry.MULTIPOINT ) );
		geom.setSRID( multiPoint.getSRID() );
		final ElemInfo info = new ElemInfo( multiPoint.getNumPoints() );
		int oordinatesOffset = 1;
		Double[] ordinates = new Double[] { };
		for ( int i = 0; i < multiPoint.getNumPoints(); i++ ) {
			info.setElement( i, oordinatesOffset, ElementType.POINT, 0 );
			ordinates = convertAddCoordinates(
					ordinates, multiPoint
					.getGeometryN( i ).getCoordinates(), dim, isLrs
			);
			oordinatesOffset = ordinates.length + 1;
		}
		geom.setInfo( info );
		geom.setOrdinates( new Ordinates( ordinates ) );
		return geom;
	}

	private SDOGeometry convertJTSPoint(Point jtsGeom) {
		final int dim = getCoordDimension( jtsGeom );
		final int lrsDim = getCoordinateLrsPosition( jtsGeom );
		final boolean isLrs = ( lrsDim != 0 );

		final Double[] coord = convertCoordinates( jtsGeom.getCoordinates(), dim, isLrs );
		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( new SDOGType( dim, lrsDim, TypeGeometry.POINT ) );
		geom.setSRID( jtsGeom.getSRID() );
		final ElemInfo info = new ElemInfo( 1 );
		info.setElement( 0, 1, ElementType.POINT, 1 );
		geom.setInfo( info );
		geom.setOrdinates( new Ordinates( coord ) );
		return geom;
	}

	private SDOGeometry convertJTSPolygon(Polygon polygon) {
		final int dim = getCoordDimension( polygon );
		final int lrsPos = getCoordinateLrsPosition( polygon );
		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( new SDOGType( dim, lrsPos, TypeGeometry.POLYGON ) );
		geom.setSRID( polygon.getSRID() );
		addPolygon( geom, polygon );
		return geom;
	}

	private void addPolygon(SDOGeometry geom, Polygon polygon) {
		final int numInteriorRings = polygon.getNumInteriorRing();
		final ElemInfo info = new ElemInfo( numInteriorRings + 1 );
		int ordinatesPreviousOffset = 0;
		if ( geom.getOrdinates() != null ) {
			ordinatesPreviousOffset = geom.getOrdinates().getOrdinateArray().length;
		}
		int ordinatesOffset = ordinatesPreviousOffset + 1;
		Double[] ordinates = new Double[] { };
		for ( int i = 0; i < info.getSize(); i++ ) {
			ElementType et;
			Coordinate[] coords;
			if ( i == 0 ) {
				et = ElementType.EXTERIOR_RING_STRAIGHT_SEGMENTS;
				coords = polygon.getExteriorRing().getCoordinates();
				if ( !CGAlgorithms.isCCW( coords ) ) {
					coords = reverseRing( coords );
				}
			}
			else {
				et = ElementType.INTERIOR_RING_STRAIGHT_SEGMENTS;
				coords = polygon.getInteriorRingN( i - 1 ).getCoordinates();
				if ( CGAlgorithms.isCCW( coords ) ) {
					coords = reverseRing( coords );
				}
			}
			info.setElement( i, ordinatesOffset, et, 0 );
			ordinates = convertAddCoordinates(
					ordinates, coords, geom
					.getDimension(), geom.isLRSGeometry()
			);
			ordinatesOffset = ordinatesPreviousOffset + ordinates.length + 1;
		}
		geom.addElement( info );
		geom.addOrdinates( ordinates );
	}

	private SDOGeometry convertJTSMultiLineString(
			MultiLineString multiLineString) {
		final int dim = getCoordDimension( multiLineString );
		final int lrsDim = getCoordinateLrsPosition( multiLineString );
		final boolean isLrs = ( lrsDim != 0 );
		final SDOGeometry geom = new SDOGeometry();
		geom.setGType( new SDOGType( dim, lrsDim, TypeGeometry.MULTILINE ) );
		geom.setSRID( multiLineString.getSRID() );
		final ElemInfo info = new ElemInfo( multiLineString.getNumGeometries() );
		int oordinatesOffset = 1;
		Double[] ordinates = new Double[] { };
		for ( int i = 0; i < multiLineString.getNumGeometries(); i++ ) {
			info.setElement(
					i, oordinatesOffset,
					ElementType.LINE_STRAITH_SEGMENTS, 0
			);
			ordinates = convertAddCoordinates(
					ordinates, multiLineString
					.getGeometryN( i ).getCoordinates(), dim, isLrs
			);
			oordinatesOffset = ordinates.length + 1;
		}
		geom.setInfo( info );
		geom.setOrdinates( new Ordinates( ordinates ) );
		return geom;
	}

	private Double[] convertAddCoordinates(Double[] ordinates, Coordinate[] coordinates, int dim, boolean isLrs) {
		final Double[] no = convertCoordinates( coordinates, dim, isLrs );
		final Double[] newordinates = new Double[ordinates.length + no.length];
		System.arraycopy( ordinates, 0, newordinates, 0, ordinates.length );
		System.arraycopy( no, 0, newordinates, ordinates.length, no.length );
		return newordinates;
	}

	/**
	 * Convert the coordinates to a double array for purposes of persisting them
	 * to the database. Note that Double.NaN values are to be converted to null
	 * values in the array.
	 *
	 * @param coordinates Coordinates to be converted to the array
	 * @param dim Coordinate dimension
	 * @param isLrs true if the coordinates contain measures
	 *
	 * @return
	 */
	private Double[] convertCoordinates(Coordinate[] coordinates, int dim,
										boolean isLrs) {

		if ( isLrs ) {
			throw new UnsupportedOperationException();
		}

		if ( dim > 4 ) {
			throw new IllegalArgumentException(
					"Dim parameter value cannot be greater than 4"
			);
		}
		final Double[] converted = new Double[coordinates.length * dim];
		for ( int i = 0; i < coordinates.length; i++ ) {
			final Coordinate c = coordinates[i];
			// set the X and Y values
			converted[i * dim] = toDouble( c.x );
			converted[i * dim + 1] = toDouble( c.y );
			if ( dim == 3 ) {
				converted[i * dim + 2] = toDouble( c.z );
			}
//			else if ( dim == 4 ) {
//				converted[i * dim + 2] = toDouble( c.z );
//				converted[i * dim + 3] = toDouble( c.m );
//			}
		}
		return converted;
	}

	/**
	 * This method converts a double primitive to a Double wrapper instance, but
	 * treats a Double.NaN value as null.
	 *
	 * @param d the value to be converted
	 *
	 * @return A Double instance of d, Null if the parameter is Double.NaN
	 */
	private Double toDouble(double d) {
		return Double.isNaN( d ) ? null : d;
	}

	/**
	 * Return the dimension required for building the gType in the SDOGeometry
	 * object. Has support for LRS type geometries.
	 *
	 * @param geom and instance of the Geometry class from which the dimension is
	 * being extracted.
	 *
	 * @return number of dimensions for purposes of creating the
	 *         SDOGeometry.SDOGType
	 */
	private int getCoordDimension(Geometry geom) {
		// This is awkward, I have to create an MCoordinate to discover what the
		// dimension is.
		// This shall be cleaner if MCoordinate.getOrdinate(int ordinateIndex)
		// is moved to the
		// Coordinate class
		final Coordinate c = geom.getCoordinate();
		int d = 0;
		if ( c != null ) {
			if ( !Double.isNaN( c.x ) ) {
				d++;
			}
			if ( !Double.isNaN( c.y ) ) {
				d++;
			}
			if ( !Double.isNaN( c.z ) ) {
				d++;
			}
//			if ( !Double.isNaN( c.m ) ) {
//				d++;
//			}
		}
		return d;
	}

	/**
	 * Returns the lrs measure position for purposes of building the gType for
	 * an oracle geometry. At this point and time, I'll have to assume that the
	 * measure is always put at the end of the ordinate tuple, even though it
	 * technically wouldn't have to. This method bases its decision on whether
	 * the first coordinate has a measure value, as measure are required for the
	 * very first and last measure in a CoordinateSequence. If there is no
	 * measure value, 0 is returned.
	 *
	 * @param geom and instance of the Geometry class from which the lrs position
	 * is being extracted.
	 *
	 * @return the lrs position for the SDOGeometry.SDOGType
	 */
	private int getCoordinateLrsPosition(Geometry geom) {
		final Coordinate c = geom.getCoordinate();
		final int measurePos = 0;
//		if ( c != null && !Double.isNaN( c.m ) ) {
//			measurePos = ( Double.isNaN( c.z ) ) ? 3 : 4;
//		}
		return measurePos;
	}

	// reverses ordinates in a coordinate array in-place

	private Coordinate[] reverseRing(Coordinate[] ar) {
		for ( int i = 0; i < ar.length / 2; i++ ) {
			final Coordinate cs = ar[i];
			ar[i] = ar[ar.length - 1 - i];
			ar[ar.length - 1 - i] = cs;
		}
		return ar;
	}

}
