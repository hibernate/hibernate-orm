/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.postgis;


import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.dialect.postgis.PGGeometryTypeDescriptor;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
public class PostgisTestSupport extends TestSupport {


	@Override
	public NativeSQLTemplates templates() {
		return new PostgisNativeSQLTemplates();
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

	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new PostgisExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new PostgisExpressionTemplate();
	}

	public GeomCodec codec() {
		return new GeomCodec() {
			@Override
			public Geometry<?> toGeometry(Object in) {
				return PGGeometryTypeDescriptor.INSTANCE_WKB_1.toGeometry( in );
			}

			@Override
			public Object fromGeometry(Geometry<?> in) {
				return Wkt.toWkt( in, Wkt.Dialect.POSTGIS_EWKT_1 );
			}
		};
	}

}
