/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.db2;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestData;
import org.hibernate.spatial.testing.TestSupport;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author David Adler, Adtech Geospatial
 * creation-date: 5/22/2014
 */
public class DB2TestSupport extends TestSupport {

	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		if ( "org.hibernate.spatial.integration.TestSpatialFunctions".equals(
				testcase.getClass().getCanonicalName() ) ) {
			return TestData.fromFile( "db2/test-db2nozm-only-polygon.xml" );
		}
		return TestData.fromFile( "db2/test-db2nozm-data-set.xml" );
	}

	public DB2ExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new DB2ExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new DB2ExpressionTemplate();
	}

}
