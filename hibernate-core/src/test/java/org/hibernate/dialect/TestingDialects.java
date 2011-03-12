/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import org.hibernate.dialect.resolver.AbstractDialectResolver;
import org.hibernate.dialect.resolver.BasicDialectResolver;
import org.hibernate.HibernateException;

/**
 * TODO : javadoc
 *
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

	public static class MyDialectResolver1 extends AbstractDialectResolver {
		protected Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
			String databaseName = metaData.getDatabaseProductName();
			int databaseMajorVersion = metaData.getDatabaseMajorVersion();
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

	public static class ErrorDialectResolver1 extends AbstractDialectResolver {
		public Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
			String databaseName = metaData.getDatabaseProductName();
			if ( databaseName.equals( "ConnectionErrorDatabase1" ) ) {
				throw new SQLException( "Simulated connection error", "08001" );
			}
			else {
				throw new SQLException();
			}
		}
	}

	public static class ErrorDialectResolver2 extends AbstractDialectResolver {
		public Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
			String databaseName = metaData.getDatabaseProductName();
			if ( databaseName.equals( "ErrorDatabase1" ) ) {
				throw new SQLException();
			}
			if ( databaseName.equals( "ErrorDatabase2" ) ) {
				throw new HibernateException( "This is a trap!" );
			}
			return null;
		}
	}

	public static class MyOverridingDialectResolver1 extends BasicDialectResolver {
		public MyOverridingDialectResolver1() {
			super( "DB2/MySpecialPlatform", MySpecialDB2Dialect.class );
		}
	}

}
