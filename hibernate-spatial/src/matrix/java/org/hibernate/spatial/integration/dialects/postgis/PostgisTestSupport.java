package org.hibernate.spatial.integration.dialects.postgis;


import org.hibernate.spatial.integration.AbstractExpectationsFactory;
import org.hibernate.spatial.integration.DataSourceUtils;
import org.hibernate.spatial.integration.SQLExpressionTemplate;
import org.hibernate.spatial.integration.TestData;
import org.hibernate.spatial.integration.TestSupport;
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
