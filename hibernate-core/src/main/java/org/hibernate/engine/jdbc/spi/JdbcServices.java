/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.service.Service;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.StandardJdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

/**
 * Provides access to services related to JDBC operations.
 * <p>
 * These services represent shared resources that do not vary by session.
 *
 * @author Steve Ebersole
 */
public interface JdbcServices extends Service {
	/**
	 * Obtain the {@link JdbcEnvironment} backing this {@code JdbcServices} instance.
	 */
	JdbcEnvironment getJdbcEnvironment();

	/**
	 * Obtain a {@link JdbcConnectionAccess} usable from bootstrap actions
	 * (hbm2ddl.auto, {@code Dialect} resolution, etc).
	 */
	JdbcConnectionAccess getBootstrapJdbcConnectionAccess();

	/**
	 * Obtain the dialect of the database.
	 */
	Dialect getDialect();

	/**
	 * Obtain service for logging SQL statements.
	 *
	 * @return The SQL statement logger.
	 */
	SqlStatementLogger getSqlStatementLogger();

	/**
	 * Obtains the service used for marking SQL parameters
	 * @return the registered ParameterMarkerStrategy implementation.
	 */
	@Incubating
	ParameterMarkerStrategy getParameterMarkerStrategy();

	/**
	 * Obtain service for dealing with exceptions.
	 *
	 * @return The exception helper service.
	 */
	SqlExceptionHelper getSqlExceptionHelper();

	/// Obtain the stable effective JDBC metadata view.
	///
	/// Use this view for runtime support decisions. Access its raw snapshot only
	/// when diagnostics, reporting, or an integration contract specifically
	/// requires driver observations.
	///
	/// @since 8.0
	JdbcMetadata getJdbcMetadata();

	/// Obtain the raw JDBC driver and bootstrap-connection observations.
	///
	/// @return the raw extracted database metadata
	/// @deprecated Use [#getJdbcMetadata()] for effective runtime decisions. For
	/// intentional raw access, use
	/// `getJdbcMetadata().getExtractedDatabaseMetaData()`.
	@Deprecated(since = "8.0")
	ExtractedDatabaseMetaData getExtractedMetaDataSupport();

	/**
	 * Create an instance of a {@link LobCreator} appropriate for the current environment,
	 * mainly meant to account for variance between:
	 * <ul>
	 * <li>JDBC 4 (&lt;= JDK 1.6) and
	 * <li>JDBC 3 (&gt;= JDK 1.5).
	 * </ul>
	 *
	 * @param lobCreationContext The context in which the LOB is being created
	 * @return The LOB creator.
	 */
	LobCreator getLobCreator(LobCreationContext lobCreationContext);

	/**
	 * Access the executor for {@link JdbcOperationQuerySelect} operations.
	 */
	default JdbcSelectExecutor getJdbcSelectExecutor() {
		return JdbcSelectExecutorStandardImpl.INSTANCE;
	}

	/**
	 * Access the executor for {@link JdbcOperationQueryMutation} operations.
	 */
	default JdbcMutationExecutor getJdbcMutationExecutor() {
		return StandardJdbcMutationExecutor.INSTANCE;
	}
}
