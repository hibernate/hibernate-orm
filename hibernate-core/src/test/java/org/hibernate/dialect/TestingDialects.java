/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
