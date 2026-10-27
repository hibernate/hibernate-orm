package org.hibernate.tool.schema.internal.exec;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;

/**
 * @author Steve Ebersole
 */
public class ImprovedExtractionContextImpl implements ExtractionContext {
	private final ServiceRegistry serviceRegistry;
	private final JdbcEnvironment jdbcEnvironment;
	private final SqlStringGenerationContext context;
	private final DdlTransactionIsolator ddlTransactionIsolator;

	private final DatabaseObjectAccess databaseObjectAccess;

	private DatabaseMetaData jdbcDatabaseMetaData;

	public ImprovedExtractionContextImpl(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			SqlStringGenerationContext context,
			DdlTransactionIsolator ddlTransactionIsolator,
			DatabaseObjectAccess databaseObjectAccess) {
		this.serviceRegistry = serviceRegistry;
		this.jdbcEnvironment = jdbcEnvironment;
		this.context = context;
		this.ddlTransactionIsolator = ddlTransactionIsolator;
		this.databaseObjectAccess = databaseObjectAccess;
	}

	@Override
	public @Nonnull ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public @Nonnull JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	@Override
	public @Nonnull SqlStringGenerationContext getSqlStringGenerationContext() {
		return context;
	}

	@Override
	public @Nonnull Connection getJdbcConnection() {
		return ddlTransactionIsolator.getIsolatedConnection();
	}

	@Override
	public @Nonnull DatabaseMetaData getJdbcDatabaseMetaData() {
		if ( jdbcDatabaseMetaData == null ) {
			try {
				jdbcDatabaseMetaData = getJdbcConnection().getMetaData();
			}
			catch (SQLException e) {
				throw jdbcEnvironment.getSqlExceptionHelper()
						.convert( e, "Unable to obtain JDBC DatabaseMetaData" );
			}
		}
		return jdbcDatabaseMetaData;
	}

	@Override
	public @Nullable Identifier getDefaultCatalog() {
		return context.getDefaultCatalog();
	}

	@Override
	public @Nullable Identifier getDefaultSchema() {
		return context.getDefaultSchema();
	}

	@Override
	public @Nonnull DatabaseObjectAccess getDatabaseObjectAccess() {
		return databaseObjectAccess;
	}

	@Override
	public void cleanup() {
		if ( jdbcDatabaseMetaData != null ) {
			jdbcDatabaseMetaData = null;
		}
	}
}
