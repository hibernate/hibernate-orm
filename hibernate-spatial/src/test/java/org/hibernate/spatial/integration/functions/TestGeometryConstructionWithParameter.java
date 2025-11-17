/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.functions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.domain.GeomEntity;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@SessionFactory
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "See https://hibernate.atlassian.net/browse/HHH-15669")
public class TestGeometryConstructionWithParameter extends SpatialTestBase {

	final private Map<CommonSpatialFunction, String> templates = new HashMap<>();

	TestGeometryConstructionWithParameter() {
		templates.put(
				CommonSpatialFunction.ST_ENVELOPE,
				"select g from GeomEntity g where st_intersects(g.geom, st_envelope(:poly) ) = true"
		);
		templates.put(
				CommonSpatialFunction.ST_BOUNDARY,
				"select g from GeomEntity g where st_intersects(g.geom, st_boundary(:poly) ) = true"
		);
		templates.put(
				CommonSpatialFunction.ST_BUFFER,
				"select g from GeomEntity g where st_intersects(g.geom, st_buffer(:poly, 1.0) ) = true"
		);
		templates.put(
				CommonSpatialFunction.ST_CONVEXHULL,
				//TODO -- this is a degenerate case of convexhull
				"select g from GeomEntity g where st_intersects(g.geom, st_convexhull(:poly) ) = true"
		);
	}

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return TestSupport.TestDataPurpose.SpatialFunctionsData;
	}

	@TestFactory
	public Stream<DynamicTest> testFunctions() {
		return Arrays.stream( CommonSpatialFunction.values() )
				.filter( f -> f.getType() == CommonSpatialFunction.Type.CONSTRUCTION && isSupported( f )
						&& templateAvailable( f ) )
				.map( this::buildTestFunction );
	}


	private DynamicTest buildTestFunction(CommonSpatialFunction func) {
		return DynamicTest.dynamicTest( buildName( func ), buildExec( func ) );
	}

	private Executable buildExec(final CommonSpatialFunction func) {
		return () -> scope.inSession( session -> {
			String hql = templates.get( func );
			hql = adaptToDialect( session, hql );
			session.createQuery( hql, GeomEntity.class ).setParameter( "poly", filterGeometry ).getResultList();
			//we just check that this parses for now.
		} );
	}

	private String adaptToDialect(SessionImplementor session, String hql) {
		//special case for SqlServer.
		// the cast ensures that SQL Server interprets the passed object (jdbc byte array) as a geometry
		if ( session.getSessionFactory().getJdbcServices().getDialect() instanceof SQLServerDialect ) {
			hql = hql.replace( ":poly", "cast( :poly as org.geolatte.geom.Geometry)" );
		}
		return hql;
	}

	private String buildName(CommonSpatialFunction func) {
		return func.getKey().getName();
	}

	private boolean templateAvailable(CommonSpatialFunction f) {
		return templates.containsKey( f );
	}
}
