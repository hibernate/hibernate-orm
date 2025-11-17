/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.cockroachdb;

import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;
import org.hibernate.spatial.testing.dialects.postgis.PostgisNativeSQLTemplates;

import org.geolatte.geom.Geometry;

public class CockroachDBTestSupport extends TestSupport {

	@Override
	public NativeSQLTemplates templates() {
		return new PostgisNativeSQLTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new PredicateRegexes("st_geomfromewkt");
	}

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		switch ( purpose ) {
			case SpatialFunctionsData:
				return TestData.fromFile( "cockroachdb/functions-test.xml" );
			default:
				return TestData.fromFile( "cockroachdb/test-data-set.xml" );
		}
	}

	public GeomCodec codec() {
		return in -> (Geometry<?>) in;
	}

}
