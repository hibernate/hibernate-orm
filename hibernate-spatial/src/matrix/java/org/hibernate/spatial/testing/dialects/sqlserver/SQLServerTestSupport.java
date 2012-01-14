package org.hibernate.spatial.testing.dialects.sqlserver;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestData;
import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.TestSupport;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 15, 2010
 */
public class SQLServerTestSupport extends TestSupport {


	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		return TestData.fromFile( "test-data-set.xml" );
	}

	public SqlServerExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new SqlServerExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new SQLServerExpressionTemplate();
	}
}
