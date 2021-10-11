/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.cockroachdb;

import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.dialect.postgis.PGGeometryType;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;
import org.hibernate.spatial.testing.dialects.postgis.PostgisNativeSQLTemplates;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;

public class CockroachDBTestSupport extends TestSupport {

	@Override
	public NativeSQLTemplates templates() {
		return new PostgisNativeSQLTemplates();
	}

	@Override
	public PredicateRegexes predicateRegexes() {
		return new CrPredicateRegexes();
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
		return new GeomCodec() {
			@Override
			public Geometry<?> toGeometry(Object in) {
				return PGGeometryType.INSTANCE_WKB_2.toGeometry( in );
			}

			@Override
			public Object fromGeometry(Geometry<?> in) {
				return Wkt.toWkt( in, Wkt.Dialect.POSTGIS_EWKT_1 );
			}
		};
	}

}

class CrPredicateRegexes extends PredicateRegexes{ }