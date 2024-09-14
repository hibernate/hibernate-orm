/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.postgis;


import java.util.HashMap;
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
 * creation-date: Sep 30, 2010
 */
@Deprecated
public class PostgisTestSupport extends TestSupport {


	@Override
	public NativeSQLTemplates templates() {
		return new PostgisNativeSQLTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes(){ return new PredicateRegexes("st_geomfromewkt");}

	//TODO  put this in its own class (analogous to NativeSQLTemplates)
	@Override
	public Map<CommonSpatialFunction, String> hqlOverrides() {
		Map<CommonSpatialFunction, String> overrides = new HashMap<>();
		return overrides;
	}

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		switch ( purpose ) {
			case SpatialFunctionsData:
				return TestData.fromFile( "postgis-functions-test.xml" );
			default:
				return TestData.fromFile( "test-data-set.xml" );
		}
	}


	public GeomCodec codec() {
		return in -> (Geometry<?>) in;
	}

}
