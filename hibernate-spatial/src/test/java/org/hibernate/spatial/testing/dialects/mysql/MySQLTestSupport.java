/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.mysql;


import java.util.List;
import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.geolatte.geom.crs.CrsId;
import org.geolatte.geom.crs.LinearUnit;
import org.geolatte.geom.crs.ProjectedCoordinateReferenceSystem;

import static org.geolatte.geom.builder.DSL.c;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;


/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 18, 2010
 */
public class MySQLTestSupport extends TestSupport {

	ProjectedCoordinateReferenceSystem crs = CoordinateReferenceSystems.mkProjected( CrsId.valueOf( 0 ), LinearUnit.METER );

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "mysql/test-mysql-functions-data-set.xml" );
	}

	@Override
	public NativeSQLTemplates templates() {
		return new MySqlNativeSqlTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new PredicateRegexes( "st_geomfromtext" );
	}

	@Override
	public Map<CommonSpatialFunction, String> hqlOverrides() {
		return super.hqlOverrides();
	}

	@Override
	public List<CommonSpatialFunction> getExcludeFromTests() {
		List<CommonSpatialFunction> exclusions = super.getExcludeFromTests();
		//these actually work, but the st_geomfromtext normalises the interior rings on polygons/geometry collections
		//thereby invalidating the test
		//todo allow a more relaxed geometry comparison that treats rings the same regardless of CCW or CW order
		exclusions.add(CommonSpatialFunction.ST_UNION);
		exclusions.add( CommonSpatialFunction.ST_SYMDIFFERENCE );
		return exclusions;
	}

	@Override
	public Geometry<?> getFilterGeometry() {

		return polygon(
				crs,
				ring( c( 0, 0 ), c( 0, 10 ), c( 10, 10 ), c( 10, 0 ), c( 0, 0 ) )
		);
	}

	@Override
	public GeomCodec codec() {
		return in -> (Geometry<?>) in;

	}
}
