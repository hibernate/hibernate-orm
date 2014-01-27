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
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.internal.ExtractedDatabaseMetaDataImpl;
import org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameSupport;
import org.hibernate.engine.jdbc.env.spi.SQLStateType;
import org.hibernate.engine.jdbc.internal.ResultSetWrapperImpl;
import org.hibernate.engine.jdbc.spi.TypeInfo;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.testing.env.ConnectionProviderBuilder;


/**
 * Implementation of the {@link JdbcServices} contract for use by these
 * tests.
 *
 * @author Steve Ebersole
 */
public class BasicTestingJdbcServiceImpl implements JdbcServices {

	private TestingJdbcEnvironmentImpl jdbcEnvironment;

	private SqlStatementLogger sqlStatementLogger = new SqlStatementLogger( true, false );;
	private final ResultSetWrapper resultSetWrapper = ResultSetWrapperImpl.INSTANCE;

	public void start() {
	}

	public void stop() {
		release();
	}

	public void prepare(boolean allowAggressiveRelease) {
		jdbcEnvironment = new TestingJdbcEnvironmentImpl( allowAggressiveRelease );

	}

	public void release() {
		if ( jdbcEnvironment.connectionProvider instanceof Stoppable ) {
			( (Stoppable) jdbcEnvironment.connectionProvider ).stop();
		}
	}

	public ConnectionProvider getConnectionProvider() {
		return jdbcEnvironment.connectionProvider;
	}

	public Dialect getDialect() {
		return jdbcEnvironment.dialect;
	}

	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return jdbcEnvironment.getLobCreatorBuilder().buildLobCreator( lobCreationContext );	}

	public ResultSetWrapper getResultSetWrapper() {
		return resultSetWrapper;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	public SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	public SqlExceptionHelper getSqlExceptionHelper() {
		return jdbcEnvironment.exceptionHelper;
	}

	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return jdbcEnvironment.extractedDatabaseMetaData;
	}

	private static class TestingJdbcEnvironmentImpl implements JdbcEnvironment {
		private final ExtractedDatabaseMetaData extractedDatabaseMetaData = new ExtractedDatabaseMetaDataImpl( this );
		private final SqlExceptionHelper exceptionHelper = new SqlExceptionHelper();
		private final LobCreatorBuilder lobCreatorBuilder = LobCreatorBuilderImpl.makeLobCreatorBuilder();

		private ConnectionProvider connectionProvider;
		private Dialect dialect;

		private TestingJdbcEnvironmentImpl(boolean allowAggressiveRelease) {
			connectionProvider = ConnectionProviderBuilder.buildConnectionProvider( allowAggressiveRelease );
			dialect = ConnectionProviderBuilder.getCorrespondingDialect();
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public ExtractedDatabaseMetaData getExtractedDatabaseMetaData() {
			return extractedDatabaseMetaData;
		}

		@Override
		public Identifier getCurrentCatalog() {
			return null;
		}

		@Override
		public Identifier getCurrentSchema() {
			return null;
		}

		@Override
		public QualifiedObjectNameSupport getQualifiedObjectNameSupport() {
			return null;
		}

		@Override
		public IdentifierHelper getIdentifierHelper() {
			return null;
		}

		@Override
		public Set<String> getReservedWords() {
			return Collections.emptySet();
		}

		@Override
		public SqlExceptionHelper getSqlExceptionHelper() {
			return exceptionHelper;
		}

		@Override
		public LobCreatorBuilder getLobCreatorBuilder() {
			return lobCreatorBuilder;
		}

		@Override
		public TypeInfo getTypeInfoForJdbcCode(int jdbcTypeCode) {
			return null;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return null;
		}
	}
}
