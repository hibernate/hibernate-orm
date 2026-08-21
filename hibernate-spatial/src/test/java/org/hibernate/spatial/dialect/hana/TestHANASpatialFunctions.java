/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.hana;

import jakarta.persistence.Query;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.testing.SpatialFunctionalTestCase;
import org.hibernate.spatial.testing.dialects.hana.HANAExpectationsFactory;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;

//TODO - see what tests are still needed, when we update/fix the HANA spatial support
@RequiresDialect(value = HANASpatialDialect.class,
		comment = "This test tests the HANA spatial functions not covered by Hibernate Spatial, HHH-12426")
@Disabled
@Deprecated
public class TestHANASpatialFunctions extends SpatialFunctionalTestCase {

	private static final HSMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			HSMessageLogger.class,
			TestHANASpatialFunctions.class.getName()
	);

	protected HANAExpectationsFactory hanaExpectationsFactory;


	@Override
	protected HSMessageLogger getLogger() {
		return LOG;
	}

	@Test
	public void test_alphashape_on_jts(SessionFactoryScope scope) throws SQLException {
		alphashape( JTS, scope );
	}

	@Test
	public void test_alphashape_on_geolatte(SessionFactoryScope scope) throws SQLException {
		alphashape( GEOLATTE, scope );
	}

	public void alphashape(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getAlphaShape( 1 );
		String hql = format(
				Locale.ENGLISH,
				"SELECT id, alphashape(geom, 1) FROM %s where geometrytype(geom) in ('ST_Point', 'ST_MultiPoint')",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_area_on_jts(SessionFactoryScope scope) throws SQLException {
		area( JTS, scope );
	}

	@Test
	public void test_area_on_geolatte(SessionFactoryScope scope) throws SQLException {
		area( GEOLATTE, scope );
	}

	public void area(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getArea();
		String hql = format(
				"SELECT id, area(geom) FROM %s where geometrytype(geom) in ('ST_Polygon', 'ST_MultiPolygon')",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_asewkb_on_jts(SessionFactoryScope scope) throws SQLException {
		asewkb( JTS, scope );
	}

	@Test
	public void test_asewkb_on_geolatte(SessionFactoryScope scope) throws SQLException {
		asewkb( GEOLATTE, scope );
	}

	public void asewkb(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, byte[]> dbexpected = hanaExpectationsFactory.getAsEWKB();
		String hql = format( "SELECT id, asewkb(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_asewkt_on_jts(SessionFactoryScope scope) throws SQLException {
		asewkt( JTS, scope );
	}

	@Test
	public void test_asewkt_on_geolatte(SessionFactoryScope scope) throws SQLException {
		asewkt( GEOLATTE, scope );
	}

	public void asewkt(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsEWKT();
		String hql = format( "SELECT id, asewkt(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_asgeojson_on_jts(SessionFactoryScope scope) throws SQLException {
		asgeojson( JTS, scope );
	}

	@Test
	public void test_asgeojson_on_geolatte(SessionFactoryScope scope) throws SQLException {
		asgeojson( GEOLATTE, scope );
	}

	public void asgeojson(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsGeoJSON();
		String hql = format( "SELECT id, asgeojson(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_assvg_on_jts(SessionFactoryScope scope) throws SQLException {
		assvg( JTS, scope );
	}

	@Test
	public void test_assvg_on_geolatte(SessionFactoryScope scope) throws SQLException {
		assvg( GEOLATTE, scope );
	}

	public void assvg(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsSVG();
		String hql = format( "SELECT id, assvg(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_assvgaggr_on_jts(SessionFactoryScope scope) throws SQLException {
		assvgaggr( JTS, scope );
	}

	@Test
	public void test_assvgaggr_on_geolatte(SessionFactoryScope scope) throws SQLException {
		assvgaggr( GEOLATTE, scope );
	}

	public void assvgaggr(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsSVGAggr();
		String hql = format(
				"SELECT cast(count(g) as int), assvgaggr(geom) FROM %s g",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_aswkb_on_jts(SessionFactoryScope scope) throws SQLException {
		aswkb( JTS, scope );
	}

	@Test
	public void test_aswkb_on_geolatte(SessionFactoryScope scope) throws SQLException {
		aswkb( GEOLATTE, scope );
	}

	public void aswkb(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, byte[]> dbexpected = hanaExpectationsFactory.getAsWKB();
		String hql = format( "SELECT id, aswkb(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_aswkt_on_jts(SessionFactoryScope scope) throws SQLException {
		aswkt( JTS, scope );
	}

	@Test
	public void test_aswkt_on_geolatte(SessionFactoryScope scope) throws SQLException {
		aswkt( GEOLATTE, scope );
	}

	public void aswkt(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsWKT();
		String hql = format( "SELECT id, aswkt(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_convexhullaggr_on_jts(SessionFactoryScope scope) throws SQLException {
		convexhullaggr( JTS, scope );
	}

	@Test
	public void test_convexhullaggr_on_geolatte(SessionFactoryScope scope) throws SQLException {
		convexhullaggr( GEOLATTE, scope );
	}

	public void convexhullaggr(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getConvexHullAggr();
		String hql = format(
				"SELECT cast(count(g) as int), convexhullaggr(geom) FROM %s g",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_centroid_on_jts(SessionFactoryScope scope) throws SQLException {
		centroid( JTS, scope );
	}

	@Test
	public void test_centroid_on_geolatte(SessionFactoryScope scope) throws SQLException {
		centroid( GEOLATTE, scope );
	}

	public void centroid(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getCentroid();
		String hql = format(
				"SELECT id, centroid(geom) FROM %s g where geometrytype(geom) = 'ST_Polygon'",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_coorddim_on_jts(SessionFactoryScope scope) throws SQLException {
		coorddim( JTS, scope );
	}

	@Test
	public void test_coorddim_on_geolatte(SessionFactoryScope scope) throws SQLException {
		coorddim( GEOLATTE, scope );
	}

	public void coorddim(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getCoordDim();
		String hql = format( "SELECT id, coorddim(geom) FROM %s", entityName( pckg ) );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_coveredby_on_jts(SessionFactoryScope scope) throws SQLException {
		coveredby( JTS, scope );
	}

	@Test
	public void test_coveredby_on_geolatte(SessionFactoryScope scope) throws SQLException {
		coveredby( GEOLATTE, scope );
	}

	public void coveredby(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getCoveredBy( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, coveredby(geom, :filter) FROM %s where coveredby(geom, :filter) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_covers_on_jts(SessionFactoryScope scope) throws SQLException {
		covers( JTS, scope );
	}

	@Test
	public void test_covers_on_geolatte(SessionFactoryScope scope) throws SQLException {
		covers( GEOLATTE, scope );
	}

	public void covers(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getCovers( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, covers(geom, :filter) FROM %s where covers(geom, :filter) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_endpoint_on_jts(SessionFactoryScope scope) throws SQLException {
		endpoint( JTS, scope );
	}

	@Test
	public void test_endpoint_on_geolatte(SessionFactoryScope scope) throws SQLException {
		endpoint( GEOLATTE, scope );
	}

	public void endpoint(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getEndPoint();
		String hql = format(
				"SELECT id, endpoint(geom) FROM %s g where geometrytype(geom) = 'ST_LineString'",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_envelopeaggr_on_jts(SessionFactoryScope scope) throws SQLException {
		envelopeaggr( JTS, scope );
	}

	@Test
	public void test_envelopeaggr_on_geolatte(SessionFactoryScope scope) throws SQLException {
		envelopeaggr( GEOLATTE, scope );
	}

	public void envelopeaggr(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getEnvelopeAggr();
		String hql = format(
				"SELECT cast(count(g) as int), envelopeaggr(geom) FROM %s g",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_exteriorring_on_jts(SessionFactoryScope scope) throws SQLException {
		exteriorring( JTS, scope );
	}

	@Test
	public void test_exteriorring_on_geolatte(SessionFactoryScope scope) throws SQLException {
		exteriorring( GEOLATTE, scope );
	}

	public void exteriorring(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getExteriorRing();
		String hql = format(
				"SELECT id, exteriorring(geom) FROM %s g where geometrytype(geom) = 'ST_Polygon'",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_geomfromewkb_on_jts(SessionFactoryScope scope) throws SQLException {
		geomfromewkb( JTS, scope );
	}

	@Test
	public void test_geomfromewkb_on_geolatte(SessionFactoryScope scope) throws SQLException {
		geomfromewkb( GEOLATTE, scope );
	}

	public void geomfromewkb(String pckg, SessionFactoryScope scope) throws SQLException {
		WKBWriter writer = new WKBWriter( 2, true );
		byte[] ewkb = writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromEWKB( ewkb );
		String hql = format(
				"SELECT 1, cast(geomfromewkb(:param) as %s) FROM %s g",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "param", ewkb );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_geomfromewkt_on_jts(SessionFactoryScope scope) throws SQLException {
		geomfromewkt( JTS, scope );
	}

	@Test
	public void test_geomfromewkt_on_geolatte(SessionFactoryScope scope) throws SQLException {
		geomfromewkt( GEOLATTE, scope );
	}

	public void geomfromewkt(String pckg, SessionFactoryScope scope) throws SQLException {
		WKTWriter writer = new WKTWriter();
		String ewkt = "SRID=" + expectationsFactory.getTestSrid() + ";" + writer.write(
				expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromEWKT( ewkt );
		String hql = format(
				"SELECT 1, cast(geomfromewkt(:param) as %s) FROM %s g",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "param", ewkt );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_geomfromtext_on_jts(SessionFactoryScope scope) throws SQLException {
		geomfromtext( JTS, scope );
	}

	@Test
	public void test_geomfromtext_on_geolatte(SessionFactoryScope scope) throws SQLException {
		geomfromtext( GEOLATTE, scope );
	}

	public void geomfromtext(String pckg, SessionFactoryScope scope) throws SQLException {
		String text = expectationsFactory.getTestPolygon().toText();
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromText( text );
		String hql = format(
				"SELECT 1, cast(geomfromtext(:param) as %s) FROM %s g",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "param", text );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_geomfromwkb_on_jts(SessionFactoryScope scope) throws SQLException {
		geomfromwkb( JTS, scope );
	}

	@Test
	public void test_geomfromwkb_on_geolatte(SessionFactoryScope scope) throws SQLException {
		geomfromwkb( GEOLATTE, scope );
	}

	public void geomfromwkb(String pckg, SessionFactoryScope scope) throws SQLException {
		WKBWriter writer = new WKBWriter( 2, false );
		byte[] wkb = writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromWKB( wkb );
		String hql = format(
				"SELECT 1, cast(geomfromwkb(:param) as %s) FROM %s g",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "param", wkb );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_geomfromwkt_on_jts(SessionFactoryScope scope) throws SQLException {
		geomfromwkt( JTS, scope );
	}

	@Test
	public void test_geomfromwkt_on_geolatte(SessionFactoryScope scope) throws SQLException {
		geomfromwkt( GEOLATTE, scope );
	}

	public void geomfromwkt(String pckg, SessionFactoryScope scope) throws SQLException {
		WKTWriter writer = new WKTWriter();
		String wkt = writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromWKT( wkt );
		String hql = format(
				"SELECT 1, cast(geomfromwkt(:param) as %s) FROM %s g",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "param", wkt );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_geometryn_on_jts(SessionFactoryScope scope) throws SQLException {
		geometryn( JTS, scope );
	}

	@Test
	public void test_geometryn_on_geolatte(SessionFactoryScope scope) throws SQLException {
		geometryn( GEOLATTE, scope );
	}

	public void geometryn(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeometryN( 1 );
		String hql = format(
				"SELECT id, cast(geometryn(geom, :n) as %s) FROM %s g where geometrytype(geom) = 'ST_GeometryCollection'",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "n", 1 );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_interiorringn_on_jts(SessionFactoryScope scope) throws SQLException {
		interiorringn( JTS, scope );
	}

	@Test
	public void test_interiorringn_on_geolatte(SessionFactoryScope scope) throws SQLException {
		interiorringn( GEOLATTE, scope );
	}

	public void interiorringn(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getInteriorRingN( 1 );
		String hql = format(
				"SELECT id, cast(interiorringn(geom, :n) as %s) FROM %s g where geometrytype(geom) = 'ST_Polygon'",
				getGeometryTypeFromPackage( pckg ),
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "n", 1 );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_intersectionaggr_on_jts(SessionFactoryScope scope) throws SQLException {
		intersectionaggr( JTS, scope );
	}

	@Test
	public void test_intersectionaggr_on_geolatte(SessionFactoryScope scope) throws SQLException {
		intersectionaggr( GEOLATTE, scope );
	}

	public void intersectionaggr(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getIntersectionAggr();
		String hql = format(
				"SELECT cast(count(g) as int), intersectionaggr(geom) FROM %s g",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_intersectsrect_on_jts(SessionFactoryScope scope) throws SQLException {
		intersectsrect( JTS, scope );
	}

	@Test
	public void test_intersectsrect_on_geolatte(SessionFactoryScope scope) throws SQLException {
		intersectsrect( GEOLATTE, scope );
	}

	public void intersectsrect(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIntersectsRect(
				(Point) expectationsFactory.getTestPoint().reverse(),
				expectationsFactory.getTestPoint()
		);
		String hql = format(
				"SELECT id, intersectsrect(geom, :pmin, :pmax) FROM %s where intersectsrect(geom, :pmin, :pmax) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		Map<String, Object> params = createQueryParams( "pmin", expectationsFactory.getTestPoint().reverse() );
		params.put( "pmax", expectationsFactory.getTestPoint() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_is3d_on_jts(SessionFactoryScope scope) throws SQLException {
		is3d( JTS, scope );
	}

	@Test
	public void test_is3d_on_geolatte(SessionFactoryScope scope) throws SQLException {
		is3d( GEOLATTE, scope );
	}

	public void is3d(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIs3D();
		String hql = format(
				"SELECT id, is3d(geom) FROM %s where is3d(geom) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_isclosed_on_jts(SessionFactoryScope scope) throws SQLException {
		isclosed( JTS, scope );
	}

	@Test
	public void test_isclosed_on_geolatte(SessionFactoryScope scope) throws SQLException {
		isclosed( GEOLATTE, scope );
	}

	public void isclosed(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsClosed();
		String hql = format(
				"SELECT id, isclosed(geom) FROM %s where geometrytype(geom) in ('ST_LineString', 'ST_MultiLineString') and isclosed(geom) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_ismeasured_on_jts(SessionFactoryScope scope) throws SQLException {
		ismeasured( JTS, scope );
	}

	@Test
	public void test_ismeasured_on_geolatte(SessionFactoryScope scope) throws SQLException {
		ismeasured( GEOLATTE, scope );
	}

	public void ismeasured(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsMeasured();
		String hql = format(
				"SELECT id, ismeasured(geom) FROM %s where ismeasured(geom) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_isring_on_jts(SessionFactoryScope scope) throws SQLException {
		isring( JTS, scope );
	}

	@Test
	public void test_isring_on_geolatte(SessionFactoryScope scope) throws SQLException {
		isring( GEOLATTE, scope );
	}

	public void isring(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsRing();
		String hql = format(
				"SELECT id, isring(geom) FROM %s where geometrytype(geom) in ('ST_LineString') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_isvalid_on_jts(SessionFactoryScope scope) throws SQLException {
		isvalid( JTS, scope );
	}

	@Test
	public void test_isvalid_on_geolatte(SessionFactoryScope scope) throws SQLException {
		isvalid( GEOLATTE, scope );
	}

	public void isvalid(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsValid();
		String hql = format(
				"SELECT id, isvalid(geom) FROM %s where isvalid(geom) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_length_on_jts(SessionFactoryScope scope) throws SQLException {
		length( JTS, scope );
	}

	@Test
	public void test_length_on_geolatte(SessionFactoryScope scope) throws SQLException {
		length( GEOLATTE, scope );
	}

	public void length(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getLength();
		String hql = format(
				"SELECT id, length(geom) FROM %s where geometrytype(geom) in ('ST_LineString', 'ST_MultiLineString') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_m_on_jts(SessionFactoryScope scope) throws SQLException {
		m( JTS, scope );
	}

	@Test
	public void test_m_on_geolatte(SessionFactoryScope scope) throws SQLException {
		m( GEOLATTE, scope );
	}

	public void m(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getM();
		String hql = format(
				"SELECT id, m(geom) FROM %s where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_mmax_on_jts(SessionFactoryScope scope) throws SQLException {
		mmax( JTS, scope );
	}

	@Test
	public void test_mmax_on_geolatte(SessionFactoryScope scope) throws SQLException {
		mmax( GEOLATTE, scope );
	}

	public void mmax(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getMMax();
		String hql = format(
				"SELECT id, mmax(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_mmin_on_jts(SessionFactoryScope scope) throws SQLException {
		mmin( JTS, scope );
	}

	@Test
	public void test_mmin_on_geolatte(SessionFactoryScope scope) throws SQLException {
		mmin( GEOLATTE, scope );
	}

	public void mmin(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getMMin();
		String hql = format(
				"SELECT id, mmin(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_numgeometries_on_jts(SessionFactoryScope scope) throws SQLException {
		numgeometries( JTS, scope );
	}

	@Test
	public void test_numgeometries_on_geolatte(SessionFactoryScope scope) throws SQLException {
		numgeometries( GEOLATTE, scope );
	}

	public void numgeometries(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumGeometries();
		String hql = format(
				"SELECT id, numgeometries(geom) FROM %s where geometrytype(geom) in ('ST_GeometryCollection') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_numinteriorring_on_jts(SessionFactoryScope scope) throws SQLException {
		numinteriorring( JTS, scope );
	}

	@Test
	public void test_numnuminteriorring_on_geolatte(SessionFactoryScope scope) throws SQLException {
		numinteriorring( GEOLATTE, scope );
	}

	public void numinteriorring(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumInteriorRing();
		String hql = format(
				"SELECT id, numinteriorring(geom) FROM %s where geometrytype(geom) in ('ST_Polygon') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_numinteriorrings_on_jts(SessionFactoryScope scope) throws SQLException {
		numinteriorrings( JTS, scope );
	}

	@Test
	public void test_numnuminteriorrings_on_geolatte(SessionFactoryScope scope) throws SQLException {
		numinteriorrings( GEOLATTE, scope );
	}

	public void numinteriorrings(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumInteriorRings();
		String hql = format(
				"SELECT id, numinteriorrings(geom) FROM %s where geometrytype(geom) in ('ST_Polygon') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_numpoints_on_jts(SessionFactoryScope scope) throws SQLException {
		numpoints( JTS, scope );
	}

	@Test
	public void test_numpoints_on_geolatte(SessionFactoryScope scope) throws SQLException {
		numpoints( GEOLATTE, scope );
	}

	public void numpoints(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumPoints();
		String hql = format(
				"SELECT id, numpoints(geom) FROM %s where geometrytype(geom) in ('ST_LineString') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_orderingequals_on_jts(SessionFactoryScope scope) throws SQLException {
		orderingequals( JTS, scope );
	}

	@Test
	public void test_orderingequals_on_geolatte(SessionFactoryScope scope) throws SQLException {
		orderingequals( GEOLATTE, scope );
	}

	public void orderingequals(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getOrderingEquals(
				expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, orderingequals(geom, :filter) FROM %s where orderingequals(geom, :filter) = true and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	@Test
	public void test_perimeter_on_jts(SessionFactoryScope scope) throws SQLException {
		perimeter( JTS, scope );
	}

	@Test
	public void test_perimeter_on_geolatte(SessionFactoryScope scope) throws SQLException {
		perimeter( GEOLATTE, scope );
	}

	public void perimeter(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getPerimeter();
		String hql = format(
				"SELECT id, perimeter(geom) FROM %s where geometrytype(geom) in ('ST_Polygon', 'ST_MultiPolygon') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_pointonsurface_on_jts(SessionFactoryScope scope) throws SQLException {
		pointonsurface( JTS, scope );
	}

	@Test
	public void test_pointonsurface_on_geolatte(SessionFactoryScope scope) throws SQLException {
		pointonsurface( GEOLATTE, scope );
	}

	public void pointonsurface(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getPointOnSurface();
		String hql = format(
				"SELECT id, pointonsurface(geom) FROM %s where geometrytype(geom) in ('ST_Polygon', 'ST_MultiPolygon') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_pointn_on_jts(SessionFactoryScope scope) throws SQLException {
		pointn( JTS, scope );
	}

	@Test
	public void test_pointn_on_geolatte(SessionFactoryScope scope) throws SQLException {
		pointn( GEOLATTE, scope );
	}

	public void pointn(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getPointN( 1 );
		String hql = format(
				"SELECT id, pointn(geom, :n) FROM %s where geometrytype(geom) in ('ST_LineString') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		Map<String, Object> params = createQueryParams( "n", 1 );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	// ST_GEOMETRY columns are not supported
	@Test
	public void test_snaptogrid_on_jts(SessionFactoryScope scope) {
		assertThrows( SQLException.class, () -> snaptogrid( JTS, scope ) );
	}

	// ST_GEOMETRY columns are not supported
	@Test
	public void test_snaptogrid_on_geolatte(SessionFactoryScope scope) {
		assertThrows( SQLException.class, () -> snaptogrid( GEOLATTE, scope ) );
	}

	public void snaptogrid(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getSnapToGrid();
		String hql = format(
				"SELECT id, snaptogrid(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_startpoint_on_jts(SessionFactoryScope scope) throws SQLException {
		startpoint( JTS, scope );
	}

	@Test
	public void test_startpoint_on_geolatte(SessionFactoryScope scope) throws SQLException {
		startpoint( GEOLATTE, scope );
	}

	public void startpoint(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getStartPoint();
		String hql = format(
				"SELECT id, startpoint(geom) FROM %s g where geometrytype(geom) = 'ST_LineString'",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_unionaggr_on_jts(SessionFactoryScope scope) throws SQLException {
		unionaggr( JTS, scope );
	}

	@Test
	public void test_unionaggr_on_geolatte(SessionFactoryScope scope) throws SQLException {
		unionaggr( GEOLATTE, scope );
	}

	public void unionaggr(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getUnionAggr();
		String hql = format(
				"SELECT cast(count(g) as int), unionaggr(geom) FROM %s g",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_x_on_jts(SessionFactoryScope scope) throws SQLException {
		x( JTS, scope );
	}

	@Test
	public void test_x_on_geolatte(SessionFactoryScope scope) throws SQLException {
		x( GEOLATTE, scope );
	}

	public void x(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getX();
		String hql = format(
				"SELECT id, x(geom) FROM %s where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_xmax_on_jts(SessionFactoryScope scope) throws SQLException {
		xmax( JTS, scope );
	}

	@Test
	public void test_xmax_on_geolatte(SessionFactoryScope scope) throws SQLException {
		xmax( GEOLATTE, scope );
	}

	public void xmax(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getXMax();
		String hql = format(
				"SELECT id, xmax(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_xmin_on_jts(SessionFactoryScope scope) throws SQLException {
		xmin( JTS, scope );
	}

	@Test
	public void test_xmin_on_geolatte(SessionFactoryScope scope) throws SQLException {
		xmin( GEOLATTE, scope );
	}

	public void xmin(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getXMin();
		String hql = format(
				"SELECT id, xmin(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_y_on_jts(SessionFactoryScope scope) throws SQLException {
		y( JTS, scope );
	}

	@Test
	public void test_y_on_geolatte(SessionFactoryScope scope) throws SQLException {
		y( GEOLATTE, scope );
	}

	public void y(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getY();
		String hql = format(
				"SELECT id, y(geom) FROM %s where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_ymax_on_jts(SessionFactoryScope scope) throws SQLException {
		ymax( JTS, scope );
	}

	@Test
	public void test_ymax_on_geolatte(SessionFactoryScope scope) throws SQLException {
		ymax( GEOLATTE, scope );
	}

	public void ymax(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getYMax();
		String hql = format(
				"SELECT id, ymax(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_ymin_on_jts(SessionFactoryScope scope) throws SQLException {
		ymin( JTS, scope );
	}

	@Test
	public void test_ymin_on_geolatte(SessionFactoryScope scope) throws SQLException {
		ymin( GEOLATTE, scope );
	}

	public void ymin(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getYMin();
		String hql = format(
				"SELECT id, ymin(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_z_on_jts(SessionFactoryScope scope) throws SQLException {
		z( JTS, scope );
	}

	@Test
	public void test_z_on_geolatte(SessionFactoryScope scope) throws SQLException {
		z( GEOLATTE, scope );
	}

	public void z(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getZ();
		String hql = format(
				"SELECT id, z(geom) FROM %s where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				entityName( pckg ),
				expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_zmax_on_jts(SessionFactoryScope scope) throws SQLException {
		zmax( JTS, scope );
	}

	@Test
	public void test_zmax_on_geolatte(SessionFactoryScope scope) throws SQLException {
		zmax( GEOLATTE, scope );
	}

	public void zmax(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getZMax();
		String hql = format(
				"SELECT id, zmax(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_zmin_on_jts(SessionFactoryScope scope) throws SQLException {
		zmin( JTS, scope );
	}

	@Test
	public void test_zmin_on_geolatte(SessionFactoryScope scope) throws SQLException {
		zmin( GEOLATTE, scope );
	}

	public void zmin(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getZMin();
		String hql = format(
				"SELECT id, zmin(geom) FROM %s where srid(geom) = %d",
				entityName( pckg ), expectationsFactory.getTestSrid()
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg, scope );
	}

	@Test
	public void test_nestedfunction_on_jts(SessionFactoryScope scope) throws SQLException {
		nestedfunction( JTS, scope );
	}

	@Test
	public void test_nestedfunction_on_geolatte(SessionFactoryScope scope) throws SQLException {
		nestedfunction( GEOLATTE, scope );
	}

	public void nestedfunction(String pckg, SessionFactoryScope scope) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getNestedFunctionInner(
				expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, geom FROM %s g where dwithin(geom, srid(:filter, 0), 1) = true",
				entityName( pckg )
		);
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );

		dbexpected = hanaExpectationsFactory.getNestedFunctionOuter( expectationsFactory.getTestPolygon() );
		hql = format(
				"SELECT id, geom FROM %s g where dwithin(:filter, srid(geom, 0), 1) = true",
				entityName( pckg )
		);
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg, scope );
	}

	private String getGeometryTypeFromPackage(String pckg) {
		switch ( pckg ) {
			case GEOLATTE:
				return org.geolatte.geom.Geometry.class.getName();
			case JTS:
				return Geometry.class.getName();
			default:
				throw new IllegalArgumentException( "Invalid package: " + pckg );
		}
	}

	private Map<String, Object> createQueryParams(String filterParamName, Object value) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put( filterParamName, value );
		return params;
	}

	public <T> void retrieveHQLResultsAndCompare(Map<Integer, T> dbexpected, String hql, String geometryType, SessionFactoryScope scope) {
		retrieveHQLResultsAndCompare( dbexpected, hql, null, geometryType, scope );
	}

	protected <T> void retrieveHQLResultsAndCompare(
			Map<Integer, T> dbexpected,
			String hql,
			Map<String, Object> params,
			String geometryType,
			SessionFactoryScope scope) {
		Map<Integer, T> hsreceived = new HashMap<>();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( hql );
					setParameters( params, query );
					addQueryResults( hsreceived, query );
				}
		);
		compare( dbexpected, hsreceived, geometryType );
	}


	private void setParameters(Map<String, Object> params, Query query) {
		if ( params == null ) {
			return;
		}
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			query.setParameter( entry.getKey(), entry.getValue() );
		}
	}

}
