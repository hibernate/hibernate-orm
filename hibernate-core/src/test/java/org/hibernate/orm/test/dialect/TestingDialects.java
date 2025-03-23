/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.BasicDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;

/**
 * @author Steve Ebersole
 */
public class TestingDialects {

	public static class MyDialect extends Dialect {
		@Override
		public DatabaseVersion getVersion() {
			return ZERO_VERSION;
		}
	}

	public static class MyDialect1 extends MyDialect {

	}

	public static class MyDialect21 extends MyDialect {
	}

	public static class MyDialect22 extends MyDialect {
	}

	public static class MyDialect311 extends MyDialect {
	}

	public static class MyDialect312 extends MyDialect {
	}

	public static class MySpecialDB2Dialect extends MyDialect {
	}

	public static class MyDialectResolver1 implements DialectResolver {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			String databaseName = info.getDatabaseName();
			int databaseMajorVersion = info.getDatabaseMajorVersion();
			if ( "MyDatabase1".equals( databaseName ) ) {
				return new MyDialect1();
			}
			if ( "MyDatabase2".equals( databaseName ) ) {
				if ( databaseMajorVersion >= 2 ) {
					return new MyDialect22();
				}
				if ( databaseMajorVersion >= 1 ) {
					return new MyDialect21();
				}
			}
			return null;
		}
	}

	public static class MyDialectResolver2 extends BasicDialectResolver {
		public MyDialectResolver2() {
			super( "MyTrickyDatabase1", MyDialect1.class );
		}
	}

	public static class MyOverridingDialectResolver1 extends BasicDialectResolver {
		public MyOverridingDialectResolver1() {
			super( "DB2/MySpecialPlatform", MySpecialDB2Dialect.class );
		}
	}

}
