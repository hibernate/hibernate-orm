package org.hibernate.spatial.dialect.postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.hibernate.spatial.jts.JTS;
import org.postgis.GeometryCollection;
import org.postgis.LineString;
import org.postgis.LinearRing;
import org.postgis.MultiLineString;
import org.postgis.MultiPoint;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import org.hibernate.spatial.jts.mgeom.MCoordinate;
import org.hibernate.spatial.jts.mgeom.MGeometry;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class PGGeometryValueBinder implements ValueBinder<Geometry> {


	@Override
	public void bind(PreparedStatement st, Geometry value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.STRUCT );
		}
		else {
			Geometry jtsGeom = (Geometry) value;
			Object dbGeom = toNative( jtsGeom, st.getConnection() );
			st.setObject( index, dbGeom );
		}
	}

	public MGeometryFactory getGeometryFactory() {
		return JTS.getDefaultGeomFactory();
	}


	/**
	 * Converts a JTS <code>Geometry</code> to a native geometry object.
	 *
	 * @param jtsGeom JTS Geometry to convert
	 * @param connection the current database connection
	 *
	 * @return native database geometry object corresponding to jtsGeom.
	 */
	private Object toNative(Geometry jtsGeom, Connection connection) {
		org.postgis.Geometry geom = null;
		jtsGeom = forceEmptyToGeometryCollection( jtsGeom );
		if ( jtsGeom instanceof com.vividsolutions.jts.geom.Point ) {
			geom = convertJTSPoint( (com.vividsolutions.jts.geom.Point) jtsGeom );
		}
		else if ( jtsGeom instanceof com.vividsolutions.jts.geom.LineString ) {
			geom = convertJTSLineString( (com.vividsolutions.jts.geom.LineString) jtsGeom );
		}
		else if ( jtsGeom instanceof com.vividsolutions.jts.geom.MultiLineString ) {
			geom = convertJTSMultiLineString( (com.vividsolutions.jts.geom.MultiLineString) jtsGeom );
		}
		else if ( jtsGeom instanceof com.vividsolutions.jts.geom.Polygon ) {
			geom = convertJTSPolygon( (com.vividsolutions.jts.geom.Polygon) jtsGeom );
		}
		else if ( jtsGeom instanceof com.vividsolutions.jts.geom.MultiPoint ) {
			geom = convertJTSMultiPoint( (com.vividsolutions.jts.geom.MultiPoint) jtsGeom );
		}
		else if ( jtsGeom instanceof com.vividsolutions.jts.geom.MultiPolygon ) {
			geom = convertJTSMultiPolygon( (com.vividsolutions.jts.geom.MultiPolygon) jtsGeom );
		}
		else if ( jtsGeom instanceof com.vividsolutions.jts.geom.GeometryCollection ) {
			geom = convertJTSGeometryCollection( (com.vividsolutions.jts.geom.GeometryCollection) jtsGeom );
		}

		if ( geom != null ) {
			return new PGgeometry( geom );
		}
		else {
			throw new UnsupportedOperationException(
					"Conversion of "
							+ jtsGeom.getClass().getSimpleName()
							+ " to PGgeometry not supported"
			);
		}
	}

	//Postgis treats every empty geometry as an empty geometrycollection

	private Geometry forceEmptyToGeometryCollection(Geometry jtsGeom) {
		Geometry forced = jtsGeom;
		if ( forced.isEmpty() ) {
			GeometryFactory factory = jtsGeom.getFactory();
			if ( factory == null ) {
				factory = JTS.getDefaultGeomFactory();
			}
			forced = factory.createGeometryCollection( null );
			forced.setSRID( jtsGeom.getSRID() );
		}
		return forced;
	}

	private MultiPolygon convertJTSMultiPolygon(
			com.vividsolutions.jts.geom.MultiPolygon multiPolygon) {
		Polygon[] pgPolygons = new Polygon[multiPolygon.getNumGeometries()];
		for ( int i = 0; i < pgPolygons.length; i++ ) {
			pgPolygons[i] = convertJTSPolygon(
					(com.vividsolutions.jts.geom.Polygon) multiPolygon
							.getGeometryN( i )
			);
		}
		MultiPolygon mpg = new MultiPolygon( pgPolygons );
		mpg.setSrid( multiPolygon.getSRID() );
		return mpg;
	}

	private MultiPoint convertJTSMultiPoint(
			com.vividsolutions.jts.geom.MultiPoint multiPoint) {
		Point[] pgPoints = new Point[multiPoint.getNumGeometries()];
		for ( int i = 0; i < pgPoints.length; i++ ) {
			pgPoints[i] = convertJTSPoint(
					(com.vividsolutions.jts.geom.Point) multiPoint
							.getGeometryN( i )
			);
		}
		MultiPoint mp = new MultiPoint( pgPoints );
		mp.setSrid( multiPoint.getSRID() );
		return mp;
	}

	private Polygon convertJTSPolygon(
			com.vividsolutions.jts.geom.Polygon jtsPolygon) {
		int numRings = jtsPolygon.getNumInteriorRing();
		org.postgis.LinearRing[] rings = new org.postgis.LinearRing[numRings + 1];
		rings[0] = convertJTSLineStringToLinearRing(
				jtsPolygon
						.getExteriorRing()
		);
		for ( int i = 0; i < numRings; i++ ) {
			rings[i + 1] = convertJTSLineStringToLinearRing(
					jtsPolygon
							.getInteriorRingN( i )
			);
		}
		Polygon polygon = new org.postgis.Polygon( rings );
		polygon.setSrid( jtsPolygon.getSRID() );
		return polygon;
	}

	private LinearRing convertJTSLineStringToLinearRing(
			com.vividsolutions.jts.geom.LineString lineString) {
		LinearRing lr = new org.postgis.LinearRing(
				toPoints(
						lineString
								.getCoordinates()
				)
		);
		lr.setSrid( lineString.getSRID() );
		return lr;
	}

	private LineString convertJTSLineString(
			com.vividsolutions.jts.geom.LineString string) {
		LineString ls = new org.postgis.LineString(
				toPoints(
						string
								.getCoordinates()
				)
		);
		if ( string instanceof MGeometry ) {
			ls.haveMeasure = true;
		}
		ls.setSrid( string.getSRID() );
		return ls;
	}

	private MultiLineString convertJTSMultiLineString(
			com.vividsolutions.jts.geom.MultiLineString string) {
		org.postgis.LineString[] lines = new org.postgis.LineString[string
				.getNumGeometries()];
		for ( int i = 0; i < string.getNumGeometries(); i++ ) {
			lines[i] = new org.postgis.LineString(
					toPoints(
							string.getGeometryN(
									i
							).getCoordinates()
					)
			);
		}
		MultiLineString mls = new MultiLineString( lines );
		if ( string instanceof MGeometry) {
			mls.haveMeasure = true;
		}
		mls.setSrid( string.getSRID() );
		return mls;
	}

	private Point convertJTSPoint(com.vividsolutions.jts.geom.Point point) {
		org.postgis.Point pgPoint = new org.postgis.Point();
		pgPoint.srid = point.getSRID();
		pgPoint.x = point.getX();
		pgPoint.y = point.getY();
		Coordinate coordinate = point.getCoordinate();
		if ( Double.isNaN( coordinate.z ) ) {
			pgPoint.dimension = 2;
		}
		else {
			pgPoint.z = coordinate.z;
			pgPoint.dimension = 3;
		}
		pgPoint.haveMeasure = false;
		if ( coordinate instanceof MCoordinate && !Double.isNaN( ( (MCoordinate) coordinate ).m ) ) {
			pgPoint.m = ( (MCoordinate) coordinate ).m;
			pgPoint.haveMeasure = true;
		}
		return pgPoint;
	}

	private GeometryCollection convertJTSGeometryCollection(
			com.vividsolutions.jts.geom.GeometryCollection collection) {
		com.vividsolutions.jts.geom.Geometry currentGeom;
		org.postgis.Geometry[] pgCollections = new org.postgis.Geometry[collection
				.getNumGeometries()];
		for ( int i = 0; i < pgCollections.length; i++ ) {
			currentGeom = collection.getGeometryN( i );
			currentGeom = forceEmptyToGeometryCollection( currentGeom );
			if ( currentGeom.getClass() == com.vividsolutions.jts.geom.LineString.class ) {
				pgCollections[i] = convertJTSLineString( (com.vividsolutions.jts.geom.LineString) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.LinearRing.class ) {
				pgCollections[i] = convertJTSLineStringToLinearRing( (com.vividsolutions.jts.geom.LinearRing) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.MultiLineString.class ) {
				pgCollections[i] = convertJTSMultiLineString( (com.vividsolutions.jts.geom.MultiLineString) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.MultiPoint.class ) {
				pgCollections[i] = convertJTSMultiPoint( (com.vividsolutions.jts.geom.MultiPoint) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.MultiPolygon.class ) {
				pgCollections[i] = convertJTSMultiPolygon( (com.vividsolutions.jts.geom.MultiPolygon) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.Point.class ) {
				pgCollections[i] = convertJTSPoint( (com.vividsolutions.jts.geom.Point) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.Polygon.class ) {
				pgCollections[i] = convertJTSPolygon( (com.vividsolutions.jts.geom.Polygon) currentGeom );
			}
			else if ( currentGeom.getClass() == com.vividsolutions.jts.geom.GeometryCollection.class ) {
				pgCollections[i] = convertJTSGeometryCollection( (com.vividsolutions.jts.geom.GeometryCollection) currentGeom );
			}
		}
		GeometryCollection gc = new GeometryCollection( pgCollections );
		gc.setSrid( collection.getSRID() );
		return gc;
	}


	private Point[] toPoints(Coordinate[] coordinates) {
		Point[] points = new Point[coordinates.length];
		for ( int i = 0; i < coordinates.length; i++ ) {
			Coordinate c = coordinates[i];
			Point pt;
			if ( Double.isNaN( c.z ) ) {
				pt = new Point( c.x, c.y );
			}
			else {
				pt = new Point( c.x, c.y, c.z );
			}
			if ( c instanceof MCoordinate ) {
				MCoordinate mc = (MCoordinate) c;
				if ( !Double.isNaN( mc.m ) ) {
					pt.setM( mc.m );
				}
			}
			points[i] = pt;
		}
		return points;
	}

}
