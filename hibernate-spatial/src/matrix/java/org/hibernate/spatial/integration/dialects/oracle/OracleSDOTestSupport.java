package org.hibernate.spatial.integration.dialects.oracle;

import org.hibernate.cfg.Configuration;
import org.hibernate.spatial.integration.AbstractExpectationsFactory;
import org.hibernate.spatial.integration.DataSourceUtils;
import org.hibernate.spatial.integration.SQLExpressionTemplate;
import org.hibernate.spatial.integration.TestData;
import org.hibernate.spatial.integration.TestSupport;
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
