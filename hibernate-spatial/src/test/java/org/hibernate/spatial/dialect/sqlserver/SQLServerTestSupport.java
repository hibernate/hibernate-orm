package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.spatial.test.DataSourceUtils;
import org.hibernate.spatial.test.SQLExpressionTemplate;
import org.hibernate.spatial.test.TestData;
import org.hibernate.spatial.test.TestSupport;
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
