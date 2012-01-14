package org.hibernate.spatial.dialect.postgis;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.jts.JTS;
import org.postgis.GeometryCollection;
import org.postgis.MultiLineString;
import org.postgis.MultiPoint;
import org.postgis.MultiPolygon;
import org.postgis.PGboxbase;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import org.hibernate.spatial.jts.mgeom.MCoordinate;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.spatial.jts.mgeom.MLineString;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class PGGeometryValueExtractor implements ValueExtractor<Geometry> {

	@Override
	public Geometry extract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		Object geomObj = rs.getObject( name );
		return toJTS( geomObj );
	}

	public MGeometryFactory getGeometryFactory() {
		return JTS.getDefaultGeomFactory();
	}

	public Geometry toJTS(Object object) {
		if ( object == null ) {
			return null;
		}

		// in some cases, Postgis returns not PGgeometry objects
		// but org.postgis.Geometry instances.
		// This has been observed when retrieving GeometryCollections
		// as the result of an SQL-operation such as Union.
		if ( object instanceof org.postgis.Geometry ) {
			object = new PGgeometry( (org.postgis.Geometry) object );
		}

		if ( object instanceof PGgeometry ) {
			PGgeometry geom = (PGgeometry) object;
			com.vividsolutions.jts.geom.Geometry out = null;
			switch ( geom.getGeoType() ) {
				case org.postgis.Geometry.POINT:
					out = convertPoint( (org.postgis.Point) geom.getGeometry() );
					break;
				case org.postgis.Geometry.LINESTRING:
					out = convertLineString(
							(org.postgis.LineString) geom
									.getGeometry()
					);
					break;
				case org.postgis.Geometry.POLYGON:
					out = convertPolygon( (org.postgis.Polygon) geom.getGeometry() );
					break;
				case org.postgis.Geometry.MULTILINESTRING:
					out = convertMultiLineString(
							(org.postgis.MultiLineString) geom
									.getGeometry()
					);
					break;
				case org.postgis.Geometry.MULTIPOINT:
					out = convertMultiPoint(
							(org.postgis.MultiPoint) geom
									.getGeometry()
					);
					break;
				case org.postgis.Geometry.MULTIPOLYGON:
					out = convertMultiPolygon(
							(org.postgis.MultiPolygon) geom
									.getGeometry()
					);
					break;
				case org.postgis.Geometry.GEOMETRYCOLLECTION:
					out = convertGeometryCollection(
							(org.postgis.GeometryCollection) geom
									.getGeometry()
					);
					break;
				default:
					throw new RuntimeException( "Unknown type of PGgeometry" );
			}
			out.setSRID( geom.getGeometry().srid );
			return out;
		}
		else if ( object instanceof org.postgis.PGboxbase ) {
			return convertBox( (org.postgis.PGboxbase) object );
		}
		else {
			throw new IllegalArgumentException(
					"Can't convert object of type "
							+ object.getClass().getCanonicalName()
			);

		}

	}

	private Geometry convertBox(PGboxbase box) {
		Point ll = box.getLLB();
		Point ur = box.getURT();
		Coordinate[] ringCoords = new Coordinate[5];
		if ( box instanceof org.postgis.PGbox2d ) {
			ringCoords[0] = new Coordinate( ll.x, ll.y );
			ringCoords[1] = new Coordinate( ur.x, ll.y );
			ringCoords[2] = new Coordinate( ur.x, ur.y );
			ringCoords[3] = new Coordinate( ll.x, ur.y );
			ringCoords[4] = new Coordinate( ll.x, ll.y );
		}
		else {
			ringCoords[0] = new Coordinate( ll.x, ll.y, ll.z );
			ringCoords[1] = new Coordinate( ur.x, ll.y, ll.z );
			ringCoords[2] = new Coordinate( ur.x, ur.y, ur.z );
			ringCoords[3] = new Coordinate( ll.x, ur.y, ur.z );
			ringCoords[4] = new Coordinate( ll.x, ll.y, ll.z );
		}
		com.vividsolutions.jts.geom.LinearRing shell = getGeometryFactory()
				.createLinearRing( ringCoords );
		return getGeometryFactory().createPolygon( shell, null );
	}

	private Geometry convertGeometryCollection(GeometryCollection collection) {
		org.postgis.Geometry[] geometries = collection.getGeometries();
		com.vividsolutions.jts.geom.Geometry[] jtsGeometries = new com.vividsolutions.jts.geom.Geometry[geometries.length];
		for ( int i = 0; i < geometries.length; i++ ) {
			jtsGeometries[i] = toJTS( geometries[i] );
			//TODO  - refactor this so the following line is not necessary
			jtsGeometries[i].setSRID( 0 ); // convert2JTS sets SRIDs, but constituent geometries in a collection must have srid  == 0
		}
		com.vividsolutions.jts.geom.GeometryCollection jtsGCollection = getGeometryFactory()
				.createGeometryCollection( jtsGeometries );
		return jtsGCollection;
	}

	private Geometry convertMultiPolygon(MultiPolygon pgMultiPolygon) {
		com.vividsolutions.jts.geom.Polygon[] polygons = new com.vividsolutions.jts.geom.Polygon[pgMultiPolygon
				.numPolygons()];

		for ( int i = 0; i < polygons.length; i++ ) {
			Polygon pgPolygon = pgMultiPolygon.getPolygon( i );
			polygons[i] = (com.vividsolutions.jts.geom.Polygon) convertPolygon( pgPolygon );
		}

		com.vividsolutions.jts.geom.MultiPolygon out = getGeometryFactory()
				.createMultiPolygon( polygons );
		return out;
	}

	private Geometry convertMultiPoint(MultiPoint pgMultiPoint) {
		com.vividsolutions.jts.geom.Point[] points = new com.vividsolutions.jts.geom.Point[pgMultiPoint
				.numPoints()];

		for ( int i = 0; i < points.length; i++ ) {
			points[i] = convertPoint( pgMultiPoint.getPoint( i ) );
		}
		com.vividsolutions.jts.geom.MultiPoint out = getGeometryFactory()
				.createMultiPoint( points );
		out.setSRID( pgMultiPoint.srid );
		return out;
	}

	private com.vividsolutions.jts.geom.Geometry convertMultiLineString(
			MultiLineString mlstr) {
		com.vividsolutions.jts.geom.MultiLineString out;
		if ( mlstr.haveMeasure ) {
			MLineString[] lstrs = new MLineString[mlstr.numLines()];
			for ( int i = 0; i < mlstr.numLines(); i++ ) {
				MCoordinate[] coordinates = toJTSCoordinates(
						mlstr.getLine( i )
								.getPoints()
				);
				lstrs[i] = getGeometryFactory().createMLineString( coordinates );
			}
			out = getGeometryFactory().createMultiMLineString( lstrs );
		}
		else {
			com.vividsolutions.jts.geom.LineString[] lstrs = new com.vividsolutions.jts.geom.LineString[mlstr
					.numLines()];
			for ( int i = 0; i < mlstr.numLines(); i++ ) {
				lstrs[i] = getGeometryFactory().createLineString(
						toJTSCoordinates( mlstr.getLine( i ).getPoints() )
				);
			}
			out = getGeometryFactory().createMultiLineString( lstrs );
		}
		return out;
	}

	protected com.vividsolutions.jts.geom.Geometry convertPolygon(
			Polygon polygon) {
		com.vividsolutions.jts.geom.LinearRing shell = getGeometryFactory()
				.createLinearRing(
						toJTSCoordinates( polygon.getRing( 0 ).getPoints() )
				);
		com.vividsolutions.jts.geom.Polygon out = null;
		if ( polygon.numRings() > 1 ) {
			com.vividsolutions.jts.geom.LinearRing[] rings = new com.vividsolutions.jts.geom.LinearRing[polygon
					.numRings() - 1];
			for ( int r = 1; r < polygon.numRings(); r++ ) {
				rings[r - 1] = getGeometryFactory().createLinearRing(
						toJTSCoordinates( polygon.getRing( r ).getPoints() )
				);
			}
			out = getGeometryFactory().createPolygon( shell, rings );
		}
		else {
			out = getGeometryFactory().createPolygon( shell, null );
		}
		return out;
	}

	protected com.vividsolutions.jts.geom.Point convertPoint(Point pnt) {
		com.vividsolutions.jts.geom.Point g = getGeometryFactory().createPoint(
				this.toJTSCoordinate( pnt )
		);
		return g;
	}

	protected com.vividsolutions.jts.geom.LineString convertLineString(
			org.postgis.LineString lstr) {
		com.vividsolutions.jts.geom.LineString out = lstr.haveMeasure ? getGeometryFactory()
				.createMLineString( toJTSCoordinates( lstr.getPoints() ) )
				: getGeometryFactory().createLineString(
				toJTSCoordinates( lstr.getPoints() )
		);
		return out;
	}

	private MCoordinate[] toJTSCoordinates(Point[] points) {
		MCoordinate[] coordinates = new MCoordinate[points.length];
		for ( int i = 0; i < points.length; i++ ) {
			coordinates[i] = this.toJTSCoordinate( points[i] );
		}
		return coordinates;
	}

	private MCoordinate toJTSCoordinate(Point pt) {
		MCoordinate mc;
		if ( pt.dimension == 2 ) {
			mc = pt.haveMeasure ? MCoordinate.create2dWithMeasure(
					pt.getX(), pt
					.getY(), pt.getM()
			) : MCoordinate.create2d(
					pt.getX(), pt
					.getY()
			);
		}
		else {
			mc = pt.haveMeasure ? MCoordinate.create3dWithMeasure(
					pt.getX(), pt
					.getY(), pt.getZ(), pt.getM()
			) : MCoordinate.create3d(
                    pt
                            .getX(), pt.getY(), pt.getZ()
            );
		}
		return mc;
	}


}
