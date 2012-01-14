package org.hibernate.spatial.testing;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.testing.dialects.oracle.OracleSDOTestSupport;
import org.hibernate.spatial.testing.dialects.postgis.PostgisTestSupport;
import org.hibernate.spatial.testing.dialects.sqlserver.SQLServerTestSupport;


/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Sep 30, 2010
 */
public class TestSupportFactories {

    private static TestSupportFactories instance = new TestSupportFactories();

    public static TestSupportFactories instance() {
        return instance;
    }

    private TestSupportFactories() {
    }


    public TestSupport getTestSupportFactory(Dialect dialect) throws InstantiationException, IllegalAccessException {
        if (dialect == null) {
            throw new IllegalArgumentException("Dialect argument is required.");
        }
        Class testSupportFactoryClass = getSupportFactoryClass(dialect);
        return instantiate(testSupportFactoryClass);

    }

    private TestSupport instantiate(Class<? extends TestSupport> testSupportFactoryClass) throws IllegalAccessException, InstantiationException {
        return testSupportFactoryClass.newInstance();
    }

    private ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    private static Class<? extends TestSupport> getSupportFactoryClass(Dialect dialect) {
        String canonicalName = dialect.getClass().getCanonicalName();
        if ("org.hibernate.spatial.dialect.postgis.PostgisDialect".equals(canonicalName)) {
            return PostgisTestSupport.class;
        }
//        if ("org.hibernate.spatial.geodb.GeoDBDialect".equals(canonicalName)) {
//            return "org.hibernate.spatial.geodb.GeoDBSupport";
//        }
        if ("org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect".equals(canonicalName)) {
            return SQLServerTestSupport.class;
        }
//        if ("org.hibernatespatial.mysql.MySQLSpatialDialect".equals(canonicalName) ||
//                "org.hibernatespatial.mysql.MySQLSpatialInnoDBDialect".equals(canonicalName)) {
//            return "org.hibernatespatial.mysql.MySQLTestSupport";
//        }
        if ("org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect".equals(canonicalName)) {
            return OracleSDOTestSupport.class;
        }
        throw new IllegalArgumentException("Dialect not known in test suite");
    }

}

