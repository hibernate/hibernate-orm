package org.hibernate.spatial.dialect.postgis;


import org.hibernate.spatial.test.AbstractExpectationsFactory;
import org.hibernate.spatial.test.DataSourceUtils;
import org.hibernate.spatial.test.SQLExpressionTemplate;
import org.hibernate.spatial.test.TestData;
import org.hibernate.spatial.test.TestSupport;
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
		return new org.hibernate.spatial.dialect.postgis.PostgisExpressionTemplate();
	}


}
