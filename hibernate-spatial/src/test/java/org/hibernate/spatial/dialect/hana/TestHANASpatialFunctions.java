package org.hibernate.spatial.dialect.hana;

import static java.lang.String.format;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.integration.TestSpatialFunctions;
import org.hibernate.spatial.testing.dialects.hana.HANAExpectationsFactory;
import org.hibernate.testing.RequiresDialect;
import org.jboss.logging.Logger;
import org.junit.Test;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTWriter;

@RequiresDialect(value = HANASpatialDialect.class, comment = "This test tests the HANA spatial functions not covered by Hibernate Spatial", jiraKey = "HHH-12426")
public class TestHANASpatialFunctions extends TestSpatialFunctions {

	private static final HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			TestHANASpatialFunctions.class.getName() );

	protected HANAExpectationsFactory hanaExpectationsFactory;

	@Override
	protected void afterConfigurationBuilt(Configuration cfg) {
		super.afterConfigurationBuilt( cfg );
		this.hanaExpectationsFactory = (HANAExpectationsFactory) this.expectationsFactory;
	}

	@Override
	protected HSMessageLogger getLogger() {
		return LOG;
	}

	@Test
	public void test_alphashape_on_jts() throws SQLException {
		alphashape( JTS );
	}

	@Test
	public void test_alphashape_on_geolatte() throws SQLException {
		alphashape( GEOLATTE );
	}

	public void alphashape(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getAlphaShape( 1 );
		String hql = format(
				"SELECT id, alphashape(geom, 1) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Point', 'ST_MultiPoint')",
				pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_area_on_jts() throws SQLException {
		area( JTS );
	}

	@Test
	public void test_area_on_geolatte() throws SQLException {
		area( GEOLATTE );
	}

	public void area(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getArea();
		String hql = format(
				"SELECT id, area(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Polygon', 'ST_MultiPolygon')",
				pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_asewkb_on_jts() throws SQLException {
		asewkb( JTS );
	}

	@Test
	public void test_asewkb_on_geolatte() throws SQLException {
		asewkb( GEOLATTE );
	}

	public void asewkb(String pckg) throws SQLException {
		Map<Integer, byte[]> dbexpected = hanaExpectationsFactory.getAsEWKB();
		String hql = format( "SELECT id, asewkb(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_asewkt_on_jts() throws SQLException {
		asewkt( JTS );
	}

	@Test
	public void test_asewkt_on_geolatte() throws SQLException {
		asewkt( GEOLATTE );
	}

	public void asewkt(String pckg) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsEWKT();
		String hql = format( "SELECT id, asewkt(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_asgeojson_on_jts() throws SQLException {
		asgeojson( JTS );
	}

	@Test
	public void test_asgeojson_on_geolatte() throws SQLException {
		asgeojson( GEOLATTE );
	}

	public void asgeojson(String pckg) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsGeoJSON();
		String hql = format( "SELECT id, asgeojson(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_assvg_on_jts() throws SQLException {
		assvg( JTS );
	}

	@Test
	public void test_assvg_on_geolatte() throws SQLException {
		assvg( GEOLATTE );
	}

	public void assvg(String pckg) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsSVG();
		String hql = format( "SELECT id, assvg(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_assvgaggr_on_jts() throws SQLException {
		assvgaggr( JTS );
	}

	@Test
	public void test_assvgaggr_on_geolatte() throws SQLException {
		assvgaggr( GEOLATTE );
	}

	public void assvgaggr(String pckg) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsSVGAggr();
		String hql = format( "SELECT cast(count(g) as int), assvgaggr(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_aswkb_on_jts() throws SQLException {
		aswkb( JTS );
	}

	@Test
	public void test_aswkb_on_geolatte() throws SQLException {
		aswkb( GEOLATTE );
	}

	public void aswkb(String pckg) throws SQLException {
		Map<Integer, byte[]> dbexpected = hanaExpectationsFactory.getAsWKB();
		String hql = format( "SELECT id, aswkb(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_aswkt_on_jts() throws SQLException {
		aswkt( JTS );
	}

	@Test
	public void test_aswkt_on_geolatte() throws SQLException {
		aswkt( GEOLATTE );
	}

	public void aswkt(String pckg) throws SQLException {
		Map<Integer, String> dbexpected = hanaExpectationsFactory.getAsWKT();
		String hql = format( "SELECT id, aswkt(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_convexhullaggr_on_jts() throws SQLException {
		convexhullaggr( JTS );
	}

	@Test
	public void test_convexhullaggr_on_geolatte() throws SQLException {
		convexhullaggr( GEOLATTE );
	}

	public void convexhullaggr(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getConvexHullAggr();
		String hql = format( "SELECT cast(count(g) as int), convexhullaggr(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_centroid_on_jts() throws SQLException {
		centroid( JTS );
	}

	@Test
	public void test_centroid_on_geolatte() throws SQLException {
		centroid( GEOLATTE );
	}

	public void centroid(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getCentroid();
		String hql = format( "SELECT id, centroid(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g where geometrytype(geom) = 'ST_Polygon'", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_coorddim_on_jts() throws SQLException {
		coorddim( JTS );
	}

	@Test
	public void test_coorddim_on_geolatte() throws SQLException {
		coorddim( GEOLATTE );
	}

	public void coorddim(String pckg) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getCoordDim();
		String hql = format( "SELECT id, coorddim(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_coveredby_on_jts() throws SQLException {
		coveredby( JTS );
	}

	@Test
	public void test_coveredby_on_geolatte() throws SQLException {
		coveredby( GEOLATTE );
	}

	public void coveredby(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getCoveredBy( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, coveredby(geom, :filter) FROM org.hibernate.spatial.integration.%s.GeomEntity where coveredby(geom, :filter) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_covers_on_jts() throws SQLException {
		covers( JTS );
	}

	@Test
	public void test_covers_on_geolatte() throws SQLException {
		covers( GEOLATTE );
	}

	public void covers(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getCovers( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, covers(geom, :filter) FROM org.hibernate.spatial.integration.%s.GeomEntity where covers(geom, :filter) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_endpoint_on_jts() throws SQLException {
		endpoint( JTS );
	}

	@Test
	public void test_endpoint_on_geolatte() throws SQLException {
		endpoint( GEOLATTE );
	}

	public void endpoint(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getEndPoint();
		String hql = format( "SELECT id, endpoint(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g where geometrytype(geom) = 'ST_LineString'",
				pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_envelopeaggr_on_jts() throws SQLException {
		envelopeaggr( JTS );
	}

	@Test
	public void test_envelopeaggr_on_geolatte() throws SQLException {
		envelopeaggr( GEOLATTE );
	}

	public void envelopeaggr(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getEnvelopeAggr();
		String hql = format( "SELECT cast(count(g) as int), envelopeaggr(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_exteriorring_on_jts() throws SQLException {
		exteriorring( JTS );
	}

	@Test
	public void test_exteriorring_on_geolatte() throws SQLException {
		exteriorring( GEOLATTE );
	}

	public void exteriorring(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getExteriorRing();
		String hql = format( "SELECT id, exteriorring(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g where geometrytype(geom) = 'ST_Polygon'",
				pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_geomfromewkb_on_jts() throws SQLException {
		geomfromewkb( JTS );
	}

	@Test
	public void test_geomfromewkb_on_geolatte() throws SQLException {
		geomfromewkb( GEOLATTE );
	}

	public void geomfromewkb(String pckg) throws SQLException {
		WKBWriter writer = new WKBWriter( 2, true );
		byte[] ewkb = writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromEWKB( ewkb );
		String hql = format( "SELECT 1, cast(geomfromewkb(:param) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "param", ewkb );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_geomfromewkt_on_jts() throws SQLException {
		geomfromewkt( JTS );
	}

	@Test
	public void test_geomfromewkt_on_geolatte() throws SQLException {
		geomfromewkt( GEOLATTE );
	}

	public void geomfromewkt(String pckg) throws SQLException {
		WKTWriter writer = new WKTWriter();
		String ewkt = "SRID=" + expectationsFactory.getTestSrid() + ";" + writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromEWKT( ewkt );
		String hql = format( "SELECT 1, cast(geomfromewkt(:param) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "param", ewkt );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_geomfromtext_on_jts() throws SQLException {
		geomfromtext( JTS );
	}

	@Test
	public void test_geomfromtext_on_geolatte() throws SQLException {
		geomfromtext( GEOLATTE );
	}

	public void geomfromtext(String pckg) throws SQLException {
		String text = expectationsFactory.getTestPolygon().toText();
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromText( text );
		String hql = format( "SELECT 1, cast(geomfromtext(:param) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "param", text );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_geomfromwkb_on_jts() throws SQLException {
		geomfromwkb( JTS );
	}

	@Test
	public void test_geomfromwkb_on_geolatte() throws SQLException {
		geomfromwkb( GEOLATTE );
	}

	public void geomfromwkb(String pckg) throws SQLException {
		WKBWriter writer = new WKBWriter( 2, false );
		byte[] wkb = writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromWKB( wkb );
		String hql = format( "SELECT 1, cast(geomfromwkb(:param) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "param", wkb );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_geomfromwkt_on_jts() throws SQLException {
		geomfromwkt( JTS );
	}

	@Test
	public void test_geomfromwkt_on_geolatte() throws SQLException {
		geomfromwkt( GEOLATTE );
	}

	public void geomfromwkt(String pckg) throws SQLException {
		WKTWriter writer = new WKTWriter();
		String wkt = writer.write( expectationsFactory.getTestPolygon() );
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeomFromWKT( wkt );
		String hql = format( "SELECT 1, cast(geomfromwkt(:param) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "param", wkt );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_geometryn_on_jts() throws SQLException {
		geometryn( JTS );
	}

	@Test
	public void test_geometryn_on_geolatte() throws SQLException {
		geometryn( GEOLATTE );
	}

	public void geometryn(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getGeometryN( 1 );
		String hql = format(
				"SELECT id, cast(geometryn(geom, :n) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g where geometrytype(geom) = 'ST_GeometryCollection'",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "n", 1 );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_interiorringn_on_jts() throws SQLException {
		interiorringn( JTS );
	}

	@Test
	public void test_interiorringn_on_geolatte() throws SQLException {
		interiorringn( GEOLATTE );
	}

	public void interiorringn(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getInteriorRingN( 1 );
		String hql = format(
				"SELECT id, cast(interiorringn(geom, :n) as %s) FROM org.hibernate.spatial.integration.%s.GeomEntity g where geometrytype(geom) = 'ST_Polygon'",
				getGeometryTypeFromPackage( pckg ), pckg );
		Map<String, Object> params = createQueryParams( "n", 1 );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_intersectionaggr_on_jts() throws SQLException {
		intersectionaggr( JTS );
	}

	@Test
	public void test_intersectionaggr_on_geolatte() throws SQLException {
		intersectionaggr( GEOLATTE );
	}

	public void intersectionaggr(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getIntersectionAggr();
		String hql = format( "SELECT cast(count(g) as int), intersectionaggr(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_intersectsrect_on_jts() throws SQLException {
		intersectsrect( JTS );
	}

	@Test
	public void test_intersectsrect_on_geolatte() throws SQLException {
		intersectsrect( GEOLATTE );
	}

	public void intersectsrect(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIntersectsRect( (Point) expectationsFactory.getTestPoint().reverse(),
				expectationsFactory.getTestPoint() );
		String hql = format(
				"SELECT id, intersectsrect(geom, :pmin, :pmax) FROM org.hibernate.spatial.integration.%s.GeomEntity where intersectsrect(geom, :pmin, :pmax) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		Map<String, Object> params = createQueryParams( "pmin", expectationsFactory.getTestPoint().reverse() );
		params.put( "pmax", expectationsFactory.getTestPoint() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_is3d_on_jts() throws SQLException {
		is3d( JTS );
	}

	@Test
	public void test_is3d_on_geolatte() throws SQLException {
		is3d( GEOLATTE );
	}

	public void is3d(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIs3D();
		String hql = format(
				"SELECT id, is3d(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where is3d(geom) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_isclosed_on_jts() throws SQLException {
		isclosed( JTS );
	}

	@Test
	public void test_isclosed_on_geolatte() throws SQLException {
		isclosed( GEOLATTE );
	}

	public void isclosed(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsClosed();
		String hql = format(
				"SELECT id, isclosed(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_LineString', 'ST_MultiLineString') and isclosed(geom) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_ismeasured_on_jts() throws SQLException {
		ismeasured( JTS );
	}

	@Test
	public void test_ismeasured_on_geolatte() throws SQLException {
		ismeasured( GEOLATTE );
	}

	public void ismeasured(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsMeasured();
		String hql = format(
				"SELECT id, ismeasured(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where ismeasured(geom) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_isring_on_jts() throws SQLException {
		isring( JTS );
	}

	@Test
	public void test_isring_on_geolatte() throws SQLException {
		isring( GEOLATTE );
	}

	public void isring(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsRing();
		String hql = format(
				"SELECT id, isring(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_LineString') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_isvalid_on_jts() throws SQLException {
		isvalid( JTS );
	}

	@Test
	public void test_isvalid_on_geolatte() throws SQLException {
		isvalid( GEOLATTE );
	}

	public void isvalid(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getIsValid();
		String hql = format(
				"SELECT id, isvalid(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where isvalid(geom) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_length_on_jts() throws SQLException {
		length( JTS );
	}

	@Test
	public void test_length_on_geolatte() throws SQLException {
		length( GEOLATTE );
	}

	public void length(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getLength();
		String hql = format(
				"SELECT id, length(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_LineString', 'ST_MultiLineString') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_m_on_jts() throws SQLException {
		m( JTS );
	}

	@Test
	public void test_m_on_geolatte() throws SQLException {
		m( GEOLATTE );
	}

	public void m(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getM();
		String hql = format(
				"SELECT id, m(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_mmax_on_jts() throws SQLException {
		mmax( JTS );
	}

	@Test
	public void test_mmax_on_geolatte() throws SQLException {
		mmax( GEOLATTE );
	}

	public void mmax(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getMMax();
		String hql = format(
				"SELECT id, mmax(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_mmin_on_jts() throws SQLException {
		mmin( JTS );
	}

	@Test
	public void test_mmin_on_geolatte() throws SQLException {
		mmin( GEOLATTE );
	}

	public void mmin(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getMMin();
		String hql = format(
				"SELECT id, mmin(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_numgeometries_on_jts() throws SQLException {
		numgeometries( JTS );
	}

	@Test
	public void test_numgeometries_on_geolatte() throws SQLException {
		numgeometries( GEOLATTE );
	}

	public void numgeometries(String pckg) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumGeometries();
		String hql = format(
				"SELECT id, numgeometries(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_GeometryCollection') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_numinteriorring_on_jts() throws SQLException {
		numinteriorring( JTS );
	}

	@Test
	public void test_numnuminteriorring_on_geolatte() throws SQLException {
		numinteriorring( GEOLATTE );
	}

	public void numinteriorring(String pckg) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumInteriorRing();
		String hql = format(
				"SELECT id, numinteriorring(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Polygon') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_numinteriorrings_on_jts() throws SQLException {
		numinteriorrings( JTS );
	}

	@Test
	public void test_numnuminteriorrings_on_geolatte() throws SQLException {
		numinteriorrings( GEOLATTE );
	}

	public void numinteriorrings(String pckg) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumInteriorRings();
		String hql = format(
				"SELECT id, numinteriorrings(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Polygon') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_numpoints_on_jts() throws SQLException {
		numpoints( JTS );
	}

	@Test
	public void test_numpoints_on_geolatte() throws SQLException {
		numpoints( GEOLATTE );
	}

	public void numpoints(String pckg) throws SQLException {
		Map<Integer, Integer> dbexpected = hanaExpectationsFactory.getNumPoints();
		String hql = format(
				"SELECT id, numpoints(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_LineString') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_orderingequals_on_jts() throws SQLException {
		orderingequals( JTS );
	}

	@Test
	public void test_orderingequals_on_geolatte() throws SQLException {
		orderingequals( GEOLATTE );
	}

	public void orderingequals(String pckg) throws SQLException {
		Map<Integer, Boolean> dbexpected = hanaExpectationsFactory.getOrderingEquals( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, orderingequals(geom, :filter) FROM org.hibernate.spatial.integration.%s.GeomEntity where orderingequals(geom, :filter) = true and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test
	public void test_perimeter_on_jts() throws SQLException {
		perimeter( JTS );
	}

	@Test
	public void test_perimeter_on_geolatte() throws SQLException {
		perimeter( GEOLATTE );
	}

	public void perimeter(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getPerimeter();
		String hql = format(
				"SELECT id, perimeter(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Polygon', 'ST_MultiPolygon') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_pointonsurface_on_jts() throws SQLException {
		pointonsurface( JTS );
	}

	@Test
	public void test_pointonsurface_on_geolatte() throws SQLException {
		pointonsurface( GEOLATTE );
	}

	public void pointonsurface(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getPointOnSurface();
		String hql = format(
				"SELECT id, pointonsurface(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Polygon', 'ST_MultiPolygon') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_pointn_on_jts() throws SQLException {
		pointn( JTS );
	}

	@Test
	public void test_pointn_on_geolatte() throws SQLException {
		pointn( GEOLATTE );
	}

	public void pointn(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getPointN( 1 );
		String hql = format(
				"SELECT id, pointn(geom, :n) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_LineString') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		Map<String, Object> params = createQueryParams( "n", 1 );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
	}

	@Test(expected = SQLException.class) // ST_GEOMETRY columns are not supported
	public void test_snaptogrid_on_jts() throws SQLException {
		snaptogrid( JTS );
	}

	@Test(expected = SQLException.class) // ST_GEOMETRY columns are not supported
	public void test_snaptogrid_on_geolatte() throws SQLException {
		snaptogrid( GEOLATTE );
	}

	public void snaptogrid(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getSnapToGrid();
		String hql = format(
				"SELECT id, snaptogrid(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_startpoint_on_jts() throws SQLException {
		startpoint( JTS );
	}

	@Test
	public void test_startpoint_on_geolatte() throws SQLException {
		startpoint( GEOLATTE );
	}

	public void startpoint(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getStartPoint();
		String hql = format( "SELECT id, startpoint(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g where geometrytype(geom) = 'ST_LineString'",
				pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_unionaggr_on_jts() throws SQLException {
		unionaggr( JTS );
	}

	@Test
	public void test_unionaggr_on_geolatte() throws SQLException {
		unionaggr( GEOLATTE );
	}

	public void unionaggr(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getUnionAggr();
		String hql = format( "SELECT cast(count(g) as int), unionaggr(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity g", pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_x_on_jts() throws SQLException {
		x( JTS );
	}

	@Test
	public void test_x_on_geolatte() throws SQLException {
		x( GEOLATTE );
	}

	public void x(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getX();
		String hql = format(
				"SELECT id, x(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_xmax_on_jts() throws SQLException {
		xmax( JTS );
	}

	@Test
	public void test_xmax_on_geolatte() throws SQLException {
		xmax( GEOLATTE );
	}

	public void xmax(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getXMax();
		String hql = format(
				"SELECT id, xmax(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_xmin_on_jts() throws SQLException {
		xmin( JTS );
	}

	@Test
	public void test_xmin_on_geolatte() throws SQLException {
		xmin( GEOLATTE );
	}

	public void xmin(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getXMin();
		String hql = format(
				"SELECT id, xmin(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_y_on_jts() throws SQLException {
		y( JTS );
	}

	@Test
	public void test_y_on_geolatte() throws SQLException {
		y( GEOLATTE );
	}

	public void y(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getY();
		String hql = format(
				"SELECT id, y(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_ymax_on_jts() throws SQLException {
		ymax( JTS );
	}

	@Test
	public void test_ymax_on_geolatte() throws SQLException {
		ymax( GEOLATTE );
	}

	public void ymax(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getYMax();
		String hql = format(
				"SELECT id, ymax(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_ymin_on_jts() throws SQLException {
		ymin( JTS );
	}

	@Test
	public void test_ymin_on_geolatte() throws SQLException {
		ymin( GEOLATTE );
	}

	public void ymin(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getYMin();
		String hql = format(
				"SELECT id, ymin(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_z_on_jts() throws SQLException {
		z( JTS );
	}

	@Test
	public void test_z_on_geolatte() throws SQLException {
		z( GEOLATTE );
	}

	public void z(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getZ();
		String hql = format(
				"SELECT id, z(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where geometrytype(geom) in ('ST_Point') and srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_zmax_on_jts() throws SQLException {
		zmax( JTS );
	}

	@Test
	public void test_zmax_on_geolatte() throws SQLException {
		zmax( GEOLATTE );
	}

	public void zmax(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getZMax();
		String hql = format(
				"SELECT id, zmax(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_zmin_on_jts() throws SQLException {
		zmin( JTS );
	}

	@Test
	public void test_zmin_on_geolatte() throws SQLException {
		zmin( GEOLATTE );
	}

	public void zmin(String pckg) throws SQLException {
		Map<Integer, Double> dbexpected = hanaExpectationsFactory.getZMin();
		String hql = format(
				"SELECT id, zmin(geom) FROM org.hibernate.spatial.integration.%s.GeomEntity where srid(geom) = %d",
				pckg, expectationsFactory.getTestSrid() );
		retrieveHQLResultsAndCompare( dbexpected, hql, pckg );
	}

	@Test
	public void test_nestedfunction_on_jts() throws SQLException {
		nestedfunction( JTS );
	}

	@Test
	public void test_nestedfunction_on_geolatte() throws SQLException {
		nestedfunction( GEOLATTE );
	}

	public void nestedfunction(String pckg) throws SQLException {
		Map<Integer, Geometry> dbexpected = hanaExpectationsFactory.getNestedFunctionInner( expectationsFactory.getTestPolygon() );
		String hql = format(
				"SELECT id, geom FROM org.hibernate.spatial.integration.%s.GeomEntity g where dwithin(geom, srid(:filter, 0), 1) = true",
				pckg );
		Map<String, Object> params = createQueryParams( "filter", expectationsFactory.getTestPolygon() );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
		
		dbexpected = hanaExpectationsFactory.getNestedFunctionOuter( expectationsFactory.getTestPolygon() );
		hql = format(
				"SELECT id, geom FROM org.hibernate.spatial.integration.%s.GeomEntity g where dwithin(:filter, srid(geom, 0), 1) = true",
				pckg );
		retrieveHQLResultsAndCompare( dbexpected, hql, params, pckg );
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
}
