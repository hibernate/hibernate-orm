package org.hibernate.spatial.dialect.oracle;

import org.hibernate.cfg.Configuration;
import org.hibernate.spatial.test.AbstractExpectationsFactory;
import org.hibernate.spatial.test.DataSourceUtils;
import org.hibernate.spatial.test.SQLExpressionTemplate;
import org.hibernate.spatial.test.TestData;
import org.hibernate.spatial.test.TestSupport;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 22, 2010
 */
public class OracleSDOTestSupport extends TestSupport {

	@Override
	public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
		return TestData.fromFile( "test-sdo-geometry-data-set-2D.xml", new SDOTestDataReader() );
	}

	@Override
	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new SDOGeometryExpectationsFactory( dataSourceUtils );
	}

	@Override
	public SQLExpressionTemplate getSQLExpressionTemplate() {
		return new SDOGeometryExpressionTemplate();
	}

	@Override
	public DataSourceUtils createDataSourceUtil(Configuration configuration) {
		this.configuration = configuration;
		return new SDODataSourceUtils( driver(), url(), user(), passwd(), getSQLExpressionTemplate() );
	}
}
