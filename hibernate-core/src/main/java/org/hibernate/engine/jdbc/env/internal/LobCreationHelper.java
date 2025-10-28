/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;

import static org.hibernate.engine.jdbc.env.internal.LobCreationLogging.LOB_LOGGER;
import static org.hibernate.engine.jdbc.env.internal.LobCreationLogging.LOB_MESSAGE_LOGGER;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Utilities for LOB creation
 *
 * @author Steve Ebersole
 */
public class LobCreationHelper {
	public static final EnumSet<LobTypes> NONE = EnumSet.noneOf( LobTypes.class );

	/**
	 * Basically here we are simply checking whether we can call the {@link Connection} methods for
	 * LOB creation added in JDBC 4.  We not only check whether the {@link Connection} declares these methods,
	 * but also whether the actual {@link Connection} instance implements them (i.e. can be called without simply
	 * throwing an exception).
	 *
	 * @param dialect The {@link Dialect} in use
	 * @param configValues The map of settings
	 * @param jdbcConnection The connection which can be used in level-of-support testing.
	 */
	public static EnumSet<LobTypes> getSupportedContextualLobTypes(Dialect dialect, Map<String,Object> configValues, Connection jdbcConnection) {
		if ( getBoolean( Environment.NON_CONTEXTUAL_LOB_CREATION, configValues ) ) {
			LOB_MESSAGE_LOGGER.disablingContextualLOBCreation( Environment.NON_CONTEXTUAL_LOB_CREATION );
			return NONE;
		}

		if ( jdbcConnection == null ) {
			LOB_MESSAGE_LOGGER.disablingContextualLOBCreationSinceConnectionNull();
			return NONE;
		}

		try {
			final var databaseMetaData = jdbcConnection.getMetaData();
			// if the jdbc driver version is less than 4, it shouldn't have createClob
			if ( databaseMetaData.getJDBCMajorVersion() < 4 ) {
				LOB_MESSAGE_LOGGER.nonContextualLobCreationJdbcVersion( databaseMetaData.getJDBCMajorVersion() );
				return NONE;
			}

			if ( !dialect.supportsJdbcConnectionLobCreation( databaseMetaData ) ) {
				LOB_MESSAGE_LOGGER.nonContextualLobCreationDialect();
				return NONE;
			}
		}
		catch (SQLException ignore) {
			// ignore exception and continue
		}

		// NOTE: for the time being, we assume that the ability to call
		// createClob() implies the ability to call createBlob()
		if ( canCreateClob( jdbcConnection ) ) {
			return canCreateNClob( jdbcConnection )
					? EnumSet.of( LobTypes.BLOB, LobTypes.CLOB, LobTypes.NCLOB )
					: EnumSet.of( LobTypes.BLOB, LobTypes.CLOB );
		}

		return NONE;
	}

	private static boolean canCreateClob(Connection jdbcConnection) {
		try {
			// We just want to see if the driver can create one
			final var clob = jdbcConnection.createClob();
			try {
				// We can immediately free it
				clob.free();
			}
			catch (Throwable e) {
				LOB_LOGGER.tracef( "Unable to free CLOB created to test createClob() implementation: %s", e );
			}
			return true;
		}
		catch (SQLException e) {
			LOB_MESSAGE_LOGGER.contextualClobCreationFailed( e );
			return false;
		}
	}

	private static boolean canCreateNClob(Connection jdbcConnection) {
		try {
			// We just want to see if the driver can create one
			final var clob = jdbcConnection.createNClob();
			try {
				// We can immediately free it
				clob.free();
			}
			catch (Throwable e) {
				LOB_LOGGER.tracef( "Unable to free NCLOB created to test createNClob() implementation: %s", e );
			}
			return true;
		}
		catch (SQLException e) {
			LOB_MESSAGE_LOGGER.contextualNClobCreationFailed( e );
			return false;
		}
	}
}
