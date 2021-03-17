/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.jdbc.dialect.spi.BasicDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/**
 * @author Steve Ebersole
 */
public class TestingDialects {

	public static class MyDialect1 extends Dialect {
	}

	public static class MyDialect21 extends Dialect {
	}

	public static class MyDialect22 extends Dialect {
	}

	public static class MyDialect311 extends Dialect {
	}

	public static class MyDialect312 extends Dialect {
	}

	public static class MySpecialDB2Dialect extends Dialect {
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
