/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.postgis;


import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestData;
import org.hibernate.spatial.testing.TestSupport;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Sep 30, 2010
 */
public class PostgisTestSupport extends TestSupport {


	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		if ( testcase.getClass().getCanonicalName().contains( "TestSpatialFunctions" ) ||
				testcase.getClass().getCanonicalName().contains( "TestSpatialRestrictions" ) ) {
			return TestData.fromFile( "postgis-functions-test.xml" );
		}
		return TestData.fromFile( "test-data-set.xml" );
	}

	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new PostgisExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new PostgisExpressionTemplate();
	}


}
