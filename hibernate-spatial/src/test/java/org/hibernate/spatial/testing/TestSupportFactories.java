/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.testing;

import org.hibernate.dialect.Dialect;
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
		if ( "org.hibernate.spatial.dialect.postgis.PostgisDialect".equals( canonicalName ) ) {
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
		if ( "org.hibernate.spatial.dialect.mysql.MySQLSpatial56Dialect".equals( canonicalName ) ||
				"org.hibernate.spatial.dialect.mysql.MySQL56InnoDBSpatialDialect".equals( canonicalName ) ) {
			return MySQL56TestSupport.class;
		}
		if ( "org.hibernate.spatial.dialect.oracle.OracleSpatial10gDialect".equals( canonicalName ) ) {
			return OracleSDOTestSupport.class;
		}
		throw new IllegalArgumentException( "Dialect not known in test suite" );
	}

}

