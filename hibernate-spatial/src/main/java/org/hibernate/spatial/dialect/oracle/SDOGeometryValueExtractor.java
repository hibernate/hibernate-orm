package org.hibernate.spatial.dialect.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.hibernate.HibernateException;
import org.hibernate.spatial.Circle;
import org.hibernate.spatial.HBSpatialExtension;
import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.spatial.mgeom.MLineString;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
public class SDOGeometryValueExtractor implements ValueExtractor<Geometry> {

	@Override
	public Geometry extract(ResultSet rs, String name, WrapperOptions options)
			throws SQLException {
		Object geomObj = rs.getObject( name );
		return toJTS( geomObj );
	}

	public MGeometryFactory getGeometryFactory() {
		return HBSpatialExtension.getDefaultGeomFactory();
	}

	public Geometry toJTS(Object struct) {
		if ( struct == null ) {
			return null;
		}
		SDOGeometry SDOGeom = SDOGeometry.load( (Struct) struct );
		return convert2JTS( SDOGeom );
	}

	private Geometry convert2JTS(SDOGeometry SDOGeom) {
		int dim = SDOGeom.getGType().getDimension();
		int lrsDim = SDOGeom.getGType().getLRSDimension();
		Geometry result = null;
		switch ( SDOGeom.getGType().getTypeGeometry() ) {
			case POINT:
				result = convertSDOPoint( SDOGeom );
				break;
			case LINE:
				result = convertSDOLine( dim, lrsDim, SDOGeom );
				break;
			case POLYGON:
				result = convertSDOPolygon( dim, lrsDim, SDOGeom );
				break;
			case MULTIPOINT:
				result = convertSDOMultiPoint( dim, lrsDim, SDOGeom );
				break;
			case MULTILINE:
				result = convertSDOMultiLine( dim, lrsDim, SDOGeom );
				break;
			case MULTIPOLYGON:
				result = convertSDOMultiPolygon( dim, lrsDim, SDOGeom );
				break;
			case COLLECTION:
				result = convertSDOCollection( dim, lrsDim, SDOGeom );
				break;
			default:
				throw new IllegalArgumentException(
						"Type not supported: "
								+ SDOGeom.getGType().getTypeGeometry()
				);
		}
		result.setSRID( SDOGeom.getSRID() );
		return result;

	}

	private Geometry convertSDOCollection(int dim, int lrsDim,
										  SDOGeometry SDOGeom) {
		List<Geometry> geometries = new ArrayList<Geometry>();
		for ( SDOGeometry elemGeom : SDOGeom.getElementGeometries() ) {
			geometries.add( convert2JTS( elemGeom ) );
		}
		Geometry[] geomArray = new Geometry[geometries.size()];
		return getGeometryFactory().createGeometryCollection(
				geometries.toArray( geomArray )
		);
	}

	private Point convertSDOPoint(SDOGeometry SDOGeom) {
		Double[] ordinates = SDOGeom.getOrdinates().getOrdinateArray();
		if ( ordinates.length == 0 ) {
			if ( SDOGeom.getDimension() == 2 ) {
				ordinates = new Double[] {
						SDOGeom.getPoint().x,
						SDOGeom.getPoint().y
				};
			}
			else {
				ordinates = new Double[] {
						SDOGeom.getPoint().x,
						SDOGeom.getPoint().y, SDOGeom.getPoint().z
				};
			}
		}
		CoordinateSequence cs = convertOrdinateArray( ordinates, SDOGeom );
		Point point = getGeometryFactory().createPoint( cs );
		return point;
	}

	private MultiPoint convertSDOMultiPoint(int dim, int lrsDim,
											SDOGeometry SDOGeom) {
		Double[] ordinates = SDOGeom.getOrdinates().getOrdinateArray();
		CoordinateSequence cs = convertOrdinateArray( ordinates, SDOGeom );
		MultiPoint multipoint = getGeometryFactory().createMultiPoint( cs );
		return multipoint;
	}

	private LineString convertSDOLine(int dim, int lrsDim, SDOGeometry SDOGeom) {
		boolean lrs = SDOGeom.isLRSGeometry();
		ElemInfo info = SDOGeom.getInfo();
		CoordinateSequence cs = null;

		int i = 0;
		while ( i < info.getSize() ) {
			if ( info.getElementType( i ).isCompound() ) {
				int numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, SDOGeom ) );
				i += 1 + numCompounds;
			}
			else {
				cs = add( cs, getElementCSeq( i, SDOGeom, false ) );
				i++;
			}
		}

		LineString ls = lrs ? getGeometryFactory().createMLineString( cs )
				: getGeometryFactory().createLineString( cs );
		return ls;
	}

	private MultiLineString convertSDOMultiLine(int dim, int lrsDim,
												SDOGeometry SDOGeom) {
		boolean lrs = SDOGeom.isLRSGeometry();
		ElemInfo info = SDOGeom.getInfo();
		LineString[] lines = lrs ? new MLineString[SDOGeom.getInfo().getSize()]
				: new LineString[SDOGeom.getInfo().getSize()];
		int i = 0;
		while ( i < info.getSize() ) {
			CoordinateSequence cs = null;
			if ( info.getElementType( i ).isCompound() ) {
				int numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, SDOGeom ) );
				LineString line = lrs ? getGeometryFactory().createMLineString(
						cs
				) : getGeometryFactory().createLineString( cs );
				lines[i] = line;
				i += 1 + numCompounds;
			}
			else {
				cs = add( cs, getElementCSeq( i, SDOGeom, false ) );
				LineString line = lrs ? getGeometryFactory().createMLineString(
						cs
				) : getGeometryFactory().createLineString( cs );
				lines[i] = line;
				i++;
			}
		}

		MultiLineString mls = lrs ? getGeometryFactory()
				.createMultiMLineString( (MLineString[]) lines )
				: getGeometryFactory().createMultiLineString( lines );
		return mls;

	}

	private Geometry convertSDOPolygon(int dim, int lrsDim, SDOGeometry SDOGeom) {
		LinearRing shell = null;
		LinearRing[] holes = new LinearRing[SDOGeom.getNumElements() - 1];
		ElemInfo info = SDOGeom.getInfo();
		int i = 0;
		int idxInteriorRings = 0;
		while ( i < info.getSize() ) {
			CoordinateSequence cs = null;
			int numCompounds = 0;
			if ( info.getElementType( i ).isCompound() ) {
				numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, SDOGeom ) );
			}
			else {
				cs = add( cs, getElementCSeq( i, SDOGeom, false ) );
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

	private MultiPolygon convertSDOMultiPolygon(int dim, int lrsDim,
												SDOGeometry SDOGeom) {
		List<LinearRing> holes = new ArrayList<LinearRing>();
		List<Polygon> polygons = new ArrayList<Polygon>();
		ElemInfo info = SDOGeom.getInfo();
		LinearRing shell = null;
		int i = 0;
		while ( i < info.getSize() ) {
			CoordinateSequence cs = null;
			int numCompounds = 0;
			if ( info.getElementType( i ).isCompound() ) {
				numCompounds = info.getNumCompounds( i );
				cs = add( cs, getCompoundCSeq( i + 1, i + numCompounds, SDOGeom ) );
			}
			else {
				cs = add( cs, getElementCSeq( i, SDOGeom, false ) );
			}
			if ( info.getElementType( i ).isInteriorRing() ) {
				LinearRing lr = getGeometryFactory().createLinearRing( cs );
				holes.add( lr );
			}
			else {
				if ( shell != null ) {
					Polygon polygon = getGeometryFactory().createPolygon(
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
			Polygon polygon = getGeometryFactory().createPolygon(
					shell,
					holes.toArray( new LinearRing[holes.size()] )
			);
			polygons.add( polygon );
		}
		MultiPolygon multiPolygon = getGeometryFactory().createMultiPolygon(
				polygons.toArray( new Polygon[polygons.size()] )
		);
		return multiPolygon;
	}

	/**
	 * Gets the CoordinateSequence corresponding to a compound element.
	 *
	 * @param idxFirst the first sub-element of the compound element
	 * @param idxLast the last sub-element of the compound element
	 * @param SDOGeom the SDOGeometry that holds the compound element.
	 *
	 * @return
	 */
	private CoordinateSequence getCompoundCSeq(int idxFirst, int idxLast,
											   SDOGeometry SDOGeom) {
		CoordinateSequence cs = null;
		for ( int i = idxFirst; i <= idxLast; i++ ) {
			// pop off the last element as it is added with the next
			// coordinate sequence
			if ( cs != null && cs.size() > 0 ) {
				Coordinate[] coordinates = cs.toCoordinateArray();
				Coordinate[] newCoordinates = new Coordinate[coordinates.length - 1];
				System.arraycopy(
						coordinates, 0, newCoordinates, 0,
						coordinates.length - 1
				);
				cs = getGeometryFactory().getCoordinateSequenceFactory()
						.create( newCoordinates );
			}
			cs = add( cs, getElementCSeq( i, SDOGeom, ( i < idxLast ) ) );
		}
		return cs;
	}

	/**
	 * Gets the CoordinateSequence corresponding to an element.
	 *
	 * @param i
	 * @param SDOGeom
	 *
	 * @return
	 */
	private CoordinateSequence getElementCSeq(int i, SDOGeometry SDOGeom,
											  boolean hasNextSE) {
		ElementType type = SDOGeom.getInfo().getElementType( i );
		Double[] elemOrdinates = extractOrdinatesOfElement(
				i, SDOGeom,
				hasNextSE
		);
		CoordinateSequence cs;
		if ( type.isStraightSegment() ) {
			cs = convertOrdinateArray( elemOrdinates, SDOGeom );
		}
		else if ( type.isArcSegment() || type.isCircle() ) {
			Coordinate[] linearized = linearize(
					elemOrdinates, SDOGeom
					.getDimension(), SDOGeom.isLRSGeometry(), type.isCircle()
			);
			cs = getGeometryFactory().getCoordinateSequenceFactory().create(
					linearized
			);
		}
		else if ( type.isRect() ) {
			cs = convertOrdinateArray( elemOrdinates, SDOGeom );
			Coordinate ll = cs.getCoordinate( 0 );
			Coordinate ur = cs.getCoordinate( 1 );
			Coordinate lr = new Coordinate( ur.x, ll.y );
			Coordinate ul = new Coordinate( ll.x, ur.y );
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

	private CoordinateSequence add(CoordinateSequence seq1,
								   CoordinateSequence seq2) {
		if ( seq1 == null ) {
			return seq2;
		}
		if ( seq2 == null ) {
			return seq1;
		}
		Coordinate[] c1 = seq1.toCoordinateArray();
		Coordinate[] c2 = seq2.toCoordinateArray();
		Coordinate[] c3 = new Coordinate[c1.length + c2.length];
		System.arraycopy( c1, 0, c3, 0, c1.length );
		System.arraycopy( c2, 0, c3, c1.length, c2.length );
		return getGeometryFactory().getCoordinateSequenceFactory().create( c3 );
	}

	private Double[] extractOrdinatesOfElement(int element,
											   SDOGeometry SDOGeom, boolean hasNextSE) {
		int start = SDOGeom.getInfo().getOrdinatesOffset( element );
		if ( element < SDOGeom.getInfo().getSize() - 1 ) {
			int end = SDOGeom.getInfo().getOrdinatesOffset( element + 1 );
			// if this is a subelement of a compound geometry,
			// the last point is the first point of
			// the next subelement.
			if ( hasNextSE ) {
				end += SDOGeom.getDimension();
			}
			return SDOGeom.getOrdinates().getOrdinatesArray( start, end );
		}
		else {
			return SDOGeom.getOrdinates().getOrdinatesArray( start );
		}
	}

	private CoordinateSequence convertOrdinateArray(Double[] oordinates,
													SDOGeometry SDOGeom) {
		int dim = SDOGeom.getDimension();
		Coordinate[] coordinates = new Coordinate[oordinates.length / dim];
		int zDim = SDOGeom.getZDimension() - 1;
		int lrsDim = SDOGeom.getLRSDimension() - 1;
		for ( int i = 0; i < coordinates.length; i++ ) {
			if ( dim == 2 ) {
				coordinates[i] = new Coordinate(
						oordinates[i * dim],
						oordinates[i * dim + 1]
				);
			}
			else if ( dim == 3 ) {
				if ( SDOGeom.isLRSGeometry() ) {
					coordinates[i] = MCoordinate.create2dWithMeasure(
							oordinates[i * dim], // X
							oordinates[i * dim + 1], // Y
							oordinates[i * dim + lrsDim]
					); // M
				}
				else {
					coordinates[i] = new Coordinate(
							oordinates[i * dim], // X
							oordinates[i * dim + 1], // Y
							oordinates[i * dim + zDim]
					); // Z
				}
			}
			else if ( dim == 4 ) {
				// This must be an LRS Geometry
				if ( !SDOGeom.isLRSGeometry() ) {
					throw new HibernateException(
							"4 dimensional Geometries must be LRS geometry"
					);
				}
				coordinates[i] = MCoordinate.create3dWithMeasure(
						oordinates[i
								* dim], // X
						oordinates[i * dim + 1], // Y
						oordinates[i * dim + zDim], // Z
						oordinates[i * dim + lrsDim]
				); // M
			}
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
	private Coordinate[] linearize(Double[] arcOrdinates, int dim, boolean lrs,
								   boolean entireCirlce) {
		Coordinate[] linearizedCoords = new Coordinate[0];
		// CoordDim is the dimension that includes only non-measure (X,Y,Z)
		// ordinates in its value
		int coordDim = lrs ? dim - 1 : dim;
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
			double x1 = arcOrdinates[numOrd++];
			double y1 = arcOrdinates[numOrd++];
			double m1 = lrs ? arcOrdinates[numOrd++] : Double.NaN;
			double x2 = arcOrdinates[numOrd++];
			double y2 = arcOrdinates[numOrd++];
			double m2 = lrs ? arcOrdinates[numOrd++] : Double.NaN;
			double x3 = arcOrdinates[numOrd++];
			double y3 = arcOrdinates[numOrd++];
			double m3 = lrs ? arcOrdinates[numOrd++] : Double.NaN;

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
				MCoordinate[] mcoord = new MCoordinate[coords.length];
				int lastIndex = coords.length - 1;
				mcoord[0] = MCoordinate.create2dWithMeasure( x1, y1, m1 );
				mcoord[lastIndex] = MCoordinate.create2dWithMeasure( x3, y3, m3 );
				// convert the middle coordinates to MCoordinate
				for ( int i = 1; i < lastIndex; i++ ) {
					mcoord[i] = MCoordinate.convertCoordinate( coords[i] );
					// if we happen to split on the middle measure, then
					// assign it
					if ( Double.compare( mcoord[i].x, x2 ) == 0
							&& Double.compare( mcoord[i].y, y2 ) == 0 ) {
						mcoord[i].m = m2;
					}
				}
				coords = mcoord;
			}

			// if this is not the first arcsegment, the first linearized
			// point is already in linearizedArc, so disregard this.
			int resultBegin = 1;
			if ( linearizedCoords.length == 0 ) {
				resultBegin = 0;
			}

			int destPos = linearizedCoords.length;
			Coordinate[] tmpCoords = new Coordinate[linearizedCoords.length
					+ coords.length - resultBegin];
			System.arraycopy(
					linearizedCoords, 0, tmpCoords, 0,
					linearizedCoords.length
			);
			System.arraycopy(
					coords, resultBegin, tmpCoords, destPos,
					coords.length - resultBegin
			);

			linearizedCoords = tmpCoords;
		}
		return linearizedCoords;
	}
}
