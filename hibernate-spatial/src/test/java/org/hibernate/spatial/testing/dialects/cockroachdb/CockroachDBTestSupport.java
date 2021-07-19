/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.cockroachdb;

import org.hibernate.spatial.integration.TestGeolatteSpatialPredicates;
import org.hibernate.spatial.integration.TestJTSSpatialPredicates;
import org.hibernate.spatial.integration.TestSpatialFunctions;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.datareader.TestData;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.postgis.PostgisExpressionTemplate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import junit.framework.TestCase;

public class CockroachDBTestSupport extends TestSupport {

	@Override
	public TestData createTestData(TestDataPurpose purpose) {
		switch ( purpose ) {
			case SpatialFunctionsData:
				return TestData.fromFile( "cockroachdb/functions-test.xml" );
			default:
				return TestData.fromFile( "cockroachdb/test-data-set.xml" );
		}
	}

	@Override
	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new CockroachDBExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new PostgisExpressionTemplate();
	}
}
