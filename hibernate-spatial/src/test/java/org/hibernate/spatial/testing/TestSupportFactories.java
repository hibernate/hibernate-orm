/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.spatial.testing.datareader.TestSupport;
import org.hibernate.spatial.testing.dialects.cockroachdb.CockroachDBTestSupport;
import org.hibernate.spatial.testing.dialects.db2.DB2TestSupport;
import org.hibernate.spatial.testing.dialects.h2gis.H2GisTestSupport;
import org.hibernate.spatial.testing.dialects.hana.HANATestSupport;
import org.hibernate.spatial.testing.dialects.mariadb.MariaDBTestSupport;
import org.hibernate.spatial.testing.dialects.mysql.MySQL8TestSupport;
import org.hibernate.spatial.testing.dialects.oracle.OracleSDOTestSupport;
import org.hibernate.spatial.testing.dialects.postgis.PostgisTestSupport;
import org.hibernate.spatial.testing.dialects.sqlserver.SQLServerTestSupport;


/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
@Deprecated
public class TestSupportFactories {

	private static final TestSupportFactories instance = new TestSupportFactories();

	private TestSupportFactories() {
	}

	public static TestSupportFactories instance() {
		return instance;
	}

	private static Class<? extends TestSupport> getSupportFactoryClass(Dialect dialect) {
		String canonicalName = dialect.getClass().getCanonicalName();
		if ( PostgreSQLDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			//this test works because all postgis dialects ultimately derive of the Postgresql82Dialect
			return PostgisTestSupport.class;
		}

		if ( MariaDBDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return MariaDBTestSupport.class;
		}

		if ( CockroachDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return CockroachDBTestSupport.class;
		}


		if ( MySQLDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return MySQL8TestSupport.class;
		}

		if ( H2Dialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return H2GisTestSupport.class;
		}

		if ( OracleDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return OracleSDOTestSupport.class;
		}

		if ( SQLServerDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			return SQLServerTestSupport.class;
		}


		if ( "org.hibernate.spatial.dialect.hana.HANASpatialDialect".equals( canonicalName ) ) {
			return HANATestSupport.class;
		}

		if ( "org.hibernate.spatial.dialect.db2.DB2SpatialDialect".equals( canonicalName ) ) {
			return DB2TestSupport.class;
		}
		throw new IllegalArgumentException( "Dialect not known in test suite" );
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

}
