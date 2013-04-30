/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.dialect.internal;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.BasicSQLExceptionConverter;
import org.hibernate.engine.jdbc.dialect.spi.AbstractDatabaseMetaDataDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseInfoDialectResolver;

/**
 * The standard Hibernate Dialect resolver.
 *
 * @author Steve Ebersole
 */
public class StandardDatabaseMetaDataDialectResolver extends AbstractDatabaseMetaDataDialectResolver {
	private final DatabaseInfoDialectResolver infoResolver;

	/**
	 * Constructs a StandardDatabaseMetaDataDialectResolver
	 *
	 * @param infoResolver The delegate resolver
	 */
	public StandardDatabaseMetaDataDialectResolver(DatabaseInfoDialectResolver infoResolver) {
		this.infoResolver = infoResolver;
	}

	/**
	 * A DatabaseInfo implementation wrapping a JDBC DatabaseMetaData reference
	 */
	public static final class DatabaseInfoImpl implements DatabaseInfoDialectResolver.DatabaseInfo {
		private final DatabaseMetaData databaseMetaData;

		protected DatabaseInfoImpl(DatabaseMetaData databaseMetaData) {
			this.databaseMetaData = databaseMetaData;
		}

		@Override
		public String getDatabaseName() {
			try {
				return databaseMetaData.getDatabaseProductName();
			}
			catch (SQLException e) {
				throw BasicSQLExceptionConverter.INSTANCE.convert( e );
			}
		}

		@Override
		public int getDatabaseMajorVersion() {
			try {
				return databaseMetaData.getDatabaseMajorVersion();
			}
			catch (SQLException e) {
				throw BasicSQLExceptionConverter.INSTANCE.convert( e );
			}
		}

		@Override
		public int getDatabaseMinorVersion() {
			try {
				return databaseMetaData.getDatabaseMinorVersion();
			}
			catch (SQLException e) {
				throw BasicSQLExceptionConverter.INSTANCE.convert( e );
			}
		}
	}

	@Override
	protected Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
		if ( infoResolver == null ) {
			return null;
		}

		return infoResolver.resolve( new DatabaseInfoImpl( metaData ) );
	}
}
