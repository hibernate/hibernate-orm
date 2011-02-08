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
package org.hibernate.test.common;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.JdbcSupport;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;
import org.hibernate.engine.jdbc.spi.SQLStatementLogger;
import org.hibernate.internal.util.jdbc.TypeInfo;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Stoppable;


/**
 * Implementation of the {@link JdbcServices} contract for use by these
 * tests.
 *
 * @author Steve Ebersole
 */
public class BasicTestingJdbcServiceImpl implements JdbcServices {
	private ConnectionProvider connectionProvider;
	private Dialect dialect;
	private SQLStatementLogger sqlStatementLogger;
	private SQLExceptionHelper exceptionHelper;
	private final ExtractedDatabaseMetaData metaDataSupport = new MetaDataSupportImpl();


	public void start() {
	}

	public void stop() {
		release();
	}

	public void prepare(boolean allowAggressiveRelease) {
		connectionProvider = ConnectionProviderBuilder.buildConnectionProvider( allowAggressiveRelease );
		dialect = ConnectionProviderBuilder.getCorrespondingDialect();
		sqlStatementLogger = new SQLStatementLogger( true, false );
		exceptionHelper = new SQLExceptionHelper();

	}

	public void release() {
		if ( connectionProvider instanceof Stoppable ) {
			( (Stoppable) connectionProvider ).stop();
		}
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public JdbcSupport getJdbcSupport() {
		return null;
	}

	public SQLStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	public SQLExceptionHelper getSqlExceptionHelper() {
		return exceptionHelper;
	}

	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return metaDataSupport;
	}

	private static class MetaDataSupportImpl implements ExtractedDatabaseMetaData {
		public boolean supportsScrollableResults() {
			return false;
		}

		public boolean supportsGetGeneratedKeys() {
			return false;
		}

		public boolean supportsBatchUpdates() {
			return false;
		}

		public boolean supportsDataDefinitionInTransaction() {
			return false;
		}

		public boolean doesDataDefinitionCauseTransactionCommit() {
			return false;
		}

		public Set<String> getExtraKeywords() {
			return Collections.emptySet();
		}

		public SQLStateType getSqlStateType() {
			return SQLStateType.UNKOWN;
		}

		public boolean doesLobLocatorUpdateCopy() {
			return false;
		}

		public String getConnectionSchemaName() {
			return null;
		}

		public String getConnectionCatalogName() {
			return null;
		}

		public LinkedHashSet<TypeInfo> getTypeInfoSet() {
			return null;
		}
	}
}
