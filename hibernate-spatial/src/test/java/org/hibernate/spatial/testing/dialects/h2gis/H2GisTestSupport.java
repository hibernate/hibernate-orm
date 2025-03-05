/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.h2gis;

import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.Geometry;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 2, 2010
 */
public class H2GisTestSupport extends TestSupport {

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "h2gis/test-data-set.xml" );
	}

	public AbstractExpectationsFactory createExpectationsFactory() {
		return new H2GISExpectationsFactory();
	}

	@Override
	public NativeSQLTemplates templates() {
		return new NativeSQLTemplates();
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
	public GeomCodec codec() {
		return in -> (Geometry<?>) in;
	}
}
