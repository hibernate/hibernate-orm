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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.hibernate.spatial.jts.Circle;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;


//TODO -- requires cleanup and must be made package local

/**
 * ValueExtractor for SDO_GEOMETRY
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
public class SDOGeometryValueExtractor<X> extends BasicExtractor<X> {

	private static GeometryFactory geometryFactory = new GeometryFactory();


	/**
	 * Creates instance
	 *
	 * @param javaDescriptor the {@code JavaTypeDescriptor} to use
	 * @param sqlTypeDescriptor the {@code SqlTypeDescriptor} to use
	 */
	public SDOGeometryValueExtractor(JavaTypeDescriptor<X> javaDescriptor, SqlTypeDescriptor sqlTypeDescriptor ) {
		super( javaDescriptor, sqlTypeDescriptor );
	}

	@Override
	protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		final Object geomObj = rs.getObject( name );
		return getJavaDescriptor().wrap( toJTS( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( index );
		return getJavaDescriptor().wrap( toJTS( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( name );
		return getJavaDescriptor().wrap( toJTS( geomObj ), options );
	}

	//TODO Clean up below this point

	protected GeometryFactory getGeometryFactory() {
		return geometryFactory;
	}

	/**
	 * Converts an oracle to a JTS Geometry
	 *
	 * @param struct The Oracle STRUCT representation of an SDO_GEOMETRY
	 *
	 * @return The JTS Geometry value
	 */
	public Geometry toJTS(Object struct) {
		if ( struct == null ) {
			return null;
		}
		final SDOGeometry sdogeom = SDOGeometry.load( (Struct) struct );
		return convert2JTS( sdogeom );
	}

	private Geometry convert2JTS(SDOGeometry sdoGeom) {
		final int dim = sdoGeom.getGType().getDimension();
		final int lrsDim = sdoGeom.getGType().getLRSDimension();
		Geometry result = null;
		switch ( sdoGeom.getGType().getTypeGeometry() ) {
			case POINT:
				result = convertSDOPoint( sdoGeom );
				break;
			case LINE:
				result = convertSDOLine( dim, lrsDim, sdoGeom );
				break;
			case POLYGON:
				result = convertSDOPolygon( dim, lrsDim, sdoGeom );
				break;
			case MULTIPOINT:
				result = convertSDOMultiPoint( dim, lrsDim, sdoGeom );
				break;
			case MULTILINE:
				result = convertSDOMultiLine( dim, lrsDim, sdoGeom );
				break;
			case MULTIPOLYGON:
				result = convertSDOMultiPolygon( dim, lrsDim, sdoGeom );
				break;
			case COLLECTION:
				result = convertSDOCollection( dim, lrsDim, sdoGeom );
				break;
			default:
				throw new IllegalArgumentException(
						"Type not supported: "
								+ sdoGeom.getGType().getTypeGeometry()
				);
		}
		result.setSRID( sdoGeom.getSRID() );
		return result;

	}

	private Geometry convertSDOCollection(int dim, int lrsDim, SDOGeometry sdoGeom) {
		final List<Geometry> geometries = new ArrayList<Geometry>();
		for ( SDOGeometry elemGeom : sdoGeom.getElementGeometries() ) {
			geometries.add( convert2JTS( elemGeom ) );
		}
		final Geometry[] geomArray = new Geometry[geometries.size()];
		return getGeometryFactory().createGeometryCollection(
				geometries.toArray( geomArray )
		);
	}

	private Point convertSDOPoint(SDOGeometry sdoGeom) {
		Double[] ordinates = sdoGeom.getOrdinates().getOrdinateArray();
		if ( ordinates.length == 0 ) {
			if ( sdoGeom.getDimension() == 2 ) {
				ordinates = new Double[] {
						sdoGeom.getPoint().x,
						sdoGeom.getPoint().y
				};
			}
			else {
				ordinates = new Double[] {
						sdoGeom.getPoint().x,
						sdoGeom.getPoint().y, sdoGeom.getPoint().z
				};
			}
		}
		final CoordinateSequence cs = convertOrdinateArray( ordinates, sdoGeom );
		return getGeometryFactory().createPoint( cs );
	}

	private MultiPoint convertSDOMultiPoint(int dim, int lrsDim, SDOGeometry sdoGeom) {
		final Double[] ordinates = sdoGeom.getOrdinates().getOrdinateArray();
		final CoordinateSequence cs = convertOrdinateArray( ordinates, sdoGeom );
		final MultiPoint multipoint = getGeometryFactory().createMultiPoint( cs );
		return multipoint;
	}

	private LineString convertSDOLine(int dim, int lrsDim, SDOGeometry sdoGeom) {
		final boolean lrs = sdoGeom.isLRSGeometry();
		final ElemInfo info = sdoGeom.getInfo();
		CoordinateSequence cs = null;

		int i = 0;
		while ( i < info.getSize() ) {
			if ( info.getElementType( i ).isCompound() ) {
				final int numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, sdoGeom ) );
				i += 1 + numCompounds;
			}
			else {
				cs = add( cs, getElementCSeq( i, sdoGeom, false ) );
				i++;
			}
		}


		if ( lrs ) {
			throw new UnsupportedOperationException();
		}
		else {
			return getGeometryFactory().createLineString( cs );
		}

	}

	private MultiLineString convertSDOMultiLine(int dim, int lrsDim, SDOGeometry sdoGeom) {
		final boolean lrs = sdoGeom.isLRSGeometry();
		if ( lrs ) {
			throw new UnsupportedOperationException();
		}
		final ElemInfo info = sdoGeom.getInfo();
		final LineString[] lines = new LineString[sdoGeom.getInfo().getSize()];
		int i = 0;
		while ( i < info.getSize() ) {
			CoordinateSequence cs = null;
			if ( info.getElementType( i ).isCompound() ) {
				final int numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, sdoGeom ) );
				final LineString line = getGeometryFactory().createLineString( cs );
				lines[i] = line;
				i += 1 + numCompounds;
			}
			else {
				cs = add( cs, getElementCSeq( i, sdoGeom, false ) );
				final LineString line = getGeometryFactory().createLineString( cs );
				lines[i] = line;
				i++;
			}
		}

		return getGeometryFactory().createMultiLineString( lines );
	}

	private Geometry convertSDOPolygon(int dim, int lrsDim, SDOGeometry sdoGeom) {
		LinearRing shell = null;
		final LinearRing[] holes = new LinearRing[sdoGeom.getNumElements() - 1];
		final ElemInfo info = sdoGeom.getInfo();
		int i = 0;
		int idxInteriorRings = 0;
		while ( i < info.getSize() ) {
			CoordinateSequence cs = null;
			int numCompounds = 0;
			if ( info.getElementType( i ).isCompound() ) {
				numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, sdoGeom ) );
			}
			else {
				cs = add( cs, getElementCSeq( i, sdoGeom, false ) );
			}
			if ( info.getElementType( i ).isInteriorRing() ) {
				holes[idxInteriorRings] = getGeometryFactory()
						.createLinearRing( cs );
				idxInteriorRings++;
			}
			else {
				shell = getGeometryFactory().createLinearRing( cs );
			}
			i += 1 + numCompounds;
		}
		return getGeometryFactory().createPolygon( shell, holes );
	}

	private MultiPolygon convertSDOMultiPolygon(int dim, int lrsDim, SDOGeometry sdoGeom) {
		List<LinearRing> holes = new ArrayList<LinearRing>();
		final List<Polygon> polygons = new ArrayList<Polygon>();
		final ElemInfo info = sdoGeom.getInfo();
		LinearRing shell = null;
		int i = 0;
		while ( i < info.getSize() ) {
			CoordinateSequence cs = null;
			int numCompounds = 0;
			if ( info.getElementType( i ).isCompound() ) {
				numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, sdoGeom ) );
			}
			else {
				cs = add( cs, getElementCSeq( i, sdoGeom, false ) );
			}
			if ( info.getElementType( i ).isInteriorRing() ) {
				final LinearRing lr = getGeometryFactory().createLinearRing( cs );
				holes.add( lr );
			}
			else {
				if ( shell != null ) {
					final Polygon polygon = getGeometryFactory().createPolygon(
							shell,
							holes.toArray( new LinearRing[holes.size()] )
					);
					polygons.add( polygon );
					shell = null;
				}
				shell = getGeometryFactory().createLinearRing( cs );
				holes = new ArrayList<LinearRing>();
			}
			i += 1 + numCompounds;
		}
		if ( shell != null ) {
			final Polygon polygon = getGeometryFactory().createPolygon(
					shell,
					holes.toArray( new LinearRing[holes.size()] )
			);
			polygons.add( polygon );
		}
		return getGeometryFactory().createMultiPolygon( polygons.toArray( new Polygon[polygons.size()] ) );
	}

	/**
	 * Gets the CoordinateSequence corresponding to a compound element.
	 *
	 * @param idxFirst the first sub-element of the compound element
	 * @param idxLast the last sub-element of the compound element
	 * @param sdoGeom the SDOGeometry that holds the compound element.
	 *
	 * @return
	 */
	private CoordinateSequence getCompoundCSeq(int idxFirst, int idxLast, SDOGeometry sdoGeom) {
		CoordinateSequence cs = null;
		for ( int i = idxFirst; i <= idxLast; i++ ) {
			// pop off the last element as it is added with the next
			// coordinate sequence
			if ( cs != null && cs.size() > 0 ) {
				final Coordinate[] coordinates = cs.toCoordinateArray();
				final Coordinate[] newCoordinates = new Coordinate[coordinates.length - 1];
				System.arraycopy( coordinates, 0, newCoordinates, 0, coordinates.length - 1 );
				cs = getGeometryFactory().getCoordinateSequenceFactory().create( newCoordinates );
			}
			cs = add( cs, getElementCSeq( i, sdoGeom, ( i < idxLast ) ) );
		}
		return cs;
	}

	/**
	 * Gets the CoordinateSequence corresponding to an element.
	 *
	 * @param i
	 * @param sdoGeom
	 *
	 * @return
	 */
	private CoordinateSequence getElementCSeq(int i, SDOGeometry sdoGeom, boolean hasNextSE) {
		final ElementType type = sdoGeom.getInfo().getElementType( i );
		final Double[] elemOrdinates = extractOrdinatesOfElement( i, sdoGeom, hasNextSE );
		CoordinateSequence cs;
		if ( type.isStraightSegment() ) {
			cs = convertOrdinateArray( elemOrdinates, sdoGeom );
		}
		else if ( type.isArcSegment() || type.isCircle() ) {
			final Coordinate[] linearized = linearize(
					elemOrdinates,
					sdoGeom.getDimension(),
					sdoGeom.isLRSGeometry(),
					type.isCircle()
			);
			cs = getGeometryFactory().getCoordinateSequenceFactory().create( linearized );
		}
		else if ( type.isRect() ) {
			cs = convertOrdinateArray( elemOrdinates, sdoGeom );
			final Coordinate ll = cs.getCoordinate( 0 );
			final Coordinate ur = cs.getCoordinate( 1 );
			final Coordinate lr = new Coordinate( ur.x, ll.y );
			final Coordinate ul = new Coordinate( ll.x, ur.y );
			if ( type.isExteriorRing() ) {
				cs = getGeometryFactory().getCoordinateSequenceFactory()
						.create( new Coordinate[] { ll, lr, ur, ul, ll } );
			}
			else {
				cs = getGeometryFactory().getCoordinateSequenceFactory()
						.create( new Coordinate[] { ll, ul, ur, lr, ll } );
			}
		}
		else {
			throw new RuntimeException(
					"Unexpected Element type in compound: "
							+ type
			);
		}
		return cs;
	}

	private CoordinateSequence add(CoordinateSequence seq1, CoordinateSequence seq2) {
		if ( seq1 == null ) {
			return seq2;
		}
		if ( seq2 == null ) {
			return seq1;
		}
		final Coordinate[] c1 = seq1.toCoordinateArray();
		final Coordinate[] c2 = seq2.toCoordinateArray();
		final Coordinate[] c3 = new Coordinate[c1.length + c2.length];
		System.arraycopy( c1, 0, c3, 0, c1.length );
		System.arraycopy( c2, 0, c3, c1.length, c2.length );
		return getGeometryFactory().getCoordinateSequenceFactory().create( c3 );
	}

	private Double[] extractOrdinatesOfElement(int element, SDOGeometry sdoGeom, boolean hasNextSE) {
		final int start = sdoGeom.getInfo().getOrdinatesOffset( element );
		if ( element < sdoGeom.getInfo().getSize() - 1 ) {
			int end = sdoGeom.getInfo().getOrdinatesOffset( element + 1 );
			// if this is a subelement of a compound geometry,
			// the last point is the first point of
			// the next subelement.
			if ( hasNextSE ) {
				end += sdoGeom.getDimension();
			}
			return sdoGeom.getOrdinates().getOrdinatesArray( start, end );
		}
		else {
			return sdoGeom.getOrdinates().getOrdinatesArray( start );
		}
	}

	private CoordinateSequence convertOrdinateArray(Double[] oordinates, SDOGeometry sdoGeom) {
		final int dim = sdoGeom.getDimension();
		final Coordinate[] coordinates = new Coordinate[oordinates.length / dim];
		final int zDim = sdoGeom.getZDimension() - 1;
		final int lrsDim = sdoGeom.getLRSDimension() - 1;
		for ( int i = 0; i < coordinates.length; i++ ) {
			if ( dim == 2 ) {
				coordinates[i] = new Coordinate(
						oordinates[i * dim],
						oordinates[i * dim + 1]
				);
			}
			else if ( dim == 3 ) {
				if ( sdoGeom.isLRSGeometry() ) {

					throw new UnsupportedOperationException();
//					coordinates[i] = MCoordinate.create2dWithMeasure(
//							oordinates[i * dim], // X
//							oordinates[i * dim + 1], // Y
//							oordinates[i * dim + lrsDim]
//					); // M
				}
				else {
					coordinates[i] = new Coordinate(
							//X
							oordinates[i * dim],
							//Y
							oordinates[i * dim + 1],
							oordinates[i * dim + zDim]
					); // Z
				}
			}
//			else if ( dim == 4 ) {
//				// This must be an LRS Geometry
//				if ( !SDOGeom.isLRSGeometry() ) {
//					throw new HibernateException(
//							"4 dimensional Geometries must be LRS geometry"
//					);
//				}
//				coordinates[i] = MCoordinate.create3dWithMeasure(
//						oordinates[i
//								* dim], // X
//						oordinates[i * dim + 1], // Y
//						oordinates[i * dim + zDim], // Z
//						oordinates[i * dim + lrsDim]
//				); // M
//			}
		}
		return getGeometryFactory().getCoordinateSequenceFactory().create(
				coordinates
		);
	}

	/**
	 * Linearizes arcs and circles.
	 *
	 * @param arcOrdinates arc or circle coordinates
	 * @param dim coordinate dimension
	 * @param lrs whether this is an lrs geometry
	 * @param entireCirlce whether the whole arc should be linearized
	 *
	 * @return linearized interpolation of arcs or circle
	 */
	private Coordinate[] linearize(Double[] arcOrdinates, int dim, boolean lrs, boolean entireCirlce) {
		Coordinate[] linearizedCoords = new Coordinate[0];
		// CoordDim is the dimension that includes only non-measure (X,Y,Z)
		// ordinates in its value
		final int coordDim = lrs ? dim - 1 : dim;
		// this only works with 2-Dimensional geometries, since we use
		// JGeometry linearization;
		if ( coordDim != 2 ) {
			throw new IllegalArgumentException(
					"Can only linearize 2D arc segments, but geometry is "
							+ dim + "D."
			);
		}
		int numOrd = dim;
		while ( numOrd < arcOrdinates.length ) {
			numOrd = numOrd - dim;
			final double x1 = arcOrdinates[numOrd++];
			final double y1 = arcOrdinates[numOrd++];
			final double m1 = lrs ? arcOrdinates[numOrd++] : Double.NaN;
			final double x2 = arcOrdinates[numOrd++];
			final double y2 = arcOrdinates[numOrd++];
			final double m2 = lrs ? arcOrdinates[numOrd++] : Double.NaN;
			final double x3 = arcOrdinates[numOrd++];
			final double y3 = arcOrdinates[numOrd++];
			final double m3 = lrs ? arcOrdinates[numOrd++] : Double.NaN;

			Coordinate[] coords;
			if ( entireCirlce ) {
				coords = Circle.linearizeCircle( x1, y1, x2, y2, x3, y3 );
			}
			else {
				coords = Circle.linearizeArc( x1, y1, x2, y2, x3, y3 );
			}

			// if this is an LRS geometry, fill the measure values into
			// the linearized array
			if ( lrs ) {
				throw new UnsupportedOperationException();
//				MCoordinate[] mcoord = new MCoordinate[coords.length];
//				int lastIndex = coords.length - 1;
//				mcoord[0] = MCoordinate.create2dWithMeasure( x1, y1, m1 );
//				mcoord[lastIndex] = MCoordinate.create2dWithMeasure( x3, y3, m3 );
//				// convert the middle coordinates to MCoordinate
//				for ( int i = 1; i < lastIndex; i++ ) {
//					mcoord[i] = MCoordinate.convertCoordinate( coords[i] );
//					// if we happen to split on the middle measure, then
//					// assign it
//					if ( Double.compare( mcoord[i].x, x2 ) == 0
//							&& Double.compare( mcoord[i].y, y2 ) == 0 ) {
//						mcoord[i].m = m2;
//					}
//				}
//				coords = mcoord;
			}

			// if this is not the first arcsegment, the first linearized
			// point is already in linearizedArc, so disregard this.
			int resultBegin = 1;
			if ( linearizedCoords.length == 0 ) {
				resultBegin = 0;
			}

			final int destPos = linearizedCoords.length;
			final Coordinate[] tmpCoords = new Coordinate[linearizedCoords.length + coords.length - resultBegin];
			System.arraycopy( linearizedCoords, 0, tmpCoords, 0, linearizedCoords.length );
			System.arraycopy( coords, resultBegin, tmpCoords, destPos, coords.length - resultBegin );
			linearizedCoords = tmpCoords;
		}
		return linearizedCoords;
	}

}
