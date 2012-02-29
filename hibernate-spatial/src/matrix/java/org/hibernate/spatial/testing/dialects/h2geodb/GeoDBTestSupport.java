package org.hibernate.spatial.testing.dialects.h2geodb;

import org.hibernate.cfg.Configuration;
import org.hibernate.spatial.testing.*;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 2, 2010
 */
public class GeoDBTestSupport extends TestSupport {


    public DataSourceUtils createDataSourceUtil(Configuration configuration) {
        super.createDataSourceUtil(configuration);
        try {
            return new GeoDBDataSourceUtils(driver(), url(), user(), passwd(), getSQLExpressionTemplate());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TestData createTestData(BaseCoreFunctionalTestCase testcase) {
        return TestData.fromFile("h2geodb/test-geodb-data-set.xml");
    }

    public GeometryEquality createGeometryEquality() {
        return new GeoDBGeometryEquality();
    }

    public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
        return new GeoDBExpectationsFactory((GeoDBDataSourceUtils) dataSourceUtils);
    }

    public SQLExpressionTemplate getSQLExpressionTemplate() {
        return new GeoDBExpressionTemplate();
    }


}

