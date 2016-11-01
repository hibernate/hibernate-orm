/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.testing.dialects.h2geodb.GeoDBTestSupport;
import org.hibernate.spatial.testing.dialects.mysql.MySQL56TestSupport;
import org.hibernate.spatial.testing.dialects.mysql.MySQLTestSupport;
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
		if ( dialect == null ) {
			throw new IllegalArgumentException( "Dialect argument is required." );
		}
		Class testSupportFactoryClass = getSupportFactoryClass( dialect );
		return instantiate( testSupportFactoryClass );

	}

	private TestSupport instantiate(Class<? extends TestSupport> testSupportFactoryClass)
			throws IllegalAccessException, InstantiationException {
		return testSupportFactoryClass.newInstance();
	}

	private ClassLoader getClassLoader() {
		return this.getClass().getClassLoader();
	}

	private static Class<? extends TestSupport> getSupportFactoryClass(Dialect dialect) {
		String canonicalName = dialect.getClass().getCanonicalName();

		if ( (dialect instanceof SpatialDialect) && PostgreSQL82Dialect.class.isAssignableFrom( dialect.getClass() ) ) {
			//this test works because all postgis dialects ultimately derive of the Postgresql82Dialect
			return PostgisTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.h2geodb.GeoDBDialect".equals( canonicalName ) ) {
			return GeoDBTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.sqlserver.SqlServer2008SpatialDialect".equals( canonicalName ) ) {
			return SQLServerTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.mysql.MySQLSpatialDialect".equals( canonicalName ) ||
				"org.hibernate.spatial.dialect.mysql.MySQL5InnoDBSpatialDialect".equals( canonicalName ) ) {
			return MySQLTestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.mysql.MySQL56SpatialDialect".equals( canonicalName ) ||
				"org.hibernate.spatial.dialect.mysql.MySQL56InnoDBSpatialDialect".equals( canonicalName ) ) {
			return MySQL56TestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect".equals( canonicalName ) ) {
			return OracleSDOTestSupport.class;
		}
		throw new IllegalArgumentException( "Dialect not known in test suite" );
	}

}
