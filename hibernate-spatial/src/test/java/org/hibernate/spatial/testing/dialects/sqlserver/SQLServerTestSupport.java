/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.sqlserver;

import java.util.List;
import java.util.Map;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;

import org.geolatte.geom.Geometry;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 15, 2010
 */
public class SQLServerTestSupport extends TestSupport {

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		return TestData.fromFile( "test-data-set.xml" );
	}

	@Override
	public NativeSQLTemplates templates() {
		return new SqlServerNativeSqlTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new SqlServerPredicateRegexes();
	}

	@Override
	public Map<CommonSpatialFunction, String> hqlOverrides() {
		return super.hqlOverrides();
	}

	@Override
	public List<CommonSpatialFunction> getExcludeFromTests() {
		//ST_Relate implementation is inconsistent accross dialects
		//TODO -- re-enable when inconsistency is resolved
		return List.of( CommonSpatialFunction.ST_RELATE );
	}

	@Override
	public GeomCodec codec() {
		return in -> (Geometry<?>) in;
	}

	@Override
	public Geometry<?> getFilterGeometry() {
		return super.getFilterGeometry();
	}
}
