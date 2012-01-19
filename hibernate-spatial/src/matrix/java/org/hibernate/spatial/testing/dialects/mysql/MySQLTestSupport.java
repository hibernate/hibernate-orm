package org.hibernate.spatial.testing.dialects.mysql;


import org.hibernate.spatial.testing.*;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 18, 2010
 */
public class MySQLTestSupport extends TestSupport {

    @Override
    public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
        if (testcase.getClass().getCanonicalName().contains("TestSpatialFunctions") ||
                testcase.getClass().getCanonicalName().contains("TestSpatialRestrictions")) {
            return TestData.fromFile("mysql/test-mysql-functions-data-set.xml");
        }
        return TestData.fromFile("test-data-set.xml");
    }

    @Override
    public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
        return new MySQLExpectationsFactory(dataSourceUtils);
    }

    @Override
    public GeometryEquality createGeometryEquality() {
        return new MySQLGeometryEquality();
    }

    @Override
    public SQLExpressionTemplate getSQLExpressionTemplate() {
        return new MySQLExpressionTemplate();
    }
}
