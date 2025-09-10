/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.util.EnumSet;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;

import static org.hibernate.engine.jdbc.env.internal.LobCreationHelper.NONE;
import static org.hibernate.engine.jdbc.env.internal.LobCreationHelper.getSupportedContextualLobTypes;
import static org.hibernate.engine.jdbc.env.internal.LobCreationLogging.LOB_LOGGER;
import static org.hibernate.engine.jdbc.env.internal.LobCreationLogging.LOB_MESSAGE_LOGGER;

/**
 * Builds {@link LobCreator} instances based on the capabilities of the environment.
 *
 * @author Steve Ebersole
 */
public class LobCreatorBuilderImpl implements LobCreatorBuilder {
	private final boolean useConnectionToCreateLob;
	private final EnumSet<LobTypes> supportedContextualLobTypes;

	public LobCreatorBuilderImpl(boolean useConnectionToCreateLob, EnumSet<LobTypes> supportedContextualLobTypes) {
		this.useConnectionToCreateLob = useConnectionToCreateLob;
		this.supportedContextualLobTypes = supportedContextualLobTypes;
	}

	// factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The public factory method for obtaining the appropriate LOB creator (according to given
	 * JDBC {@link Connection}).
	 *
	 * @param dialect The {@link Dialect} in use
	 * @param configValues The map of settings
	 * @param jdbcConnection A JDBC {@link Connection} which can be used to gauge the drivers level of support,
	 * specifically for creating LOB references.
	 */
	public static LobCreatorBuilderImpl makeLobCreatorBuilder(
			Dialect dialect,
			Map<String,Object> configValues,
			Connection jdbcConnection) {
		return new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(),
				getSupportedContextualLobTypes( dialect, configValues, jdbcConnection ) );
	}

	/**
	 * For use when JDBC {@link Connection} is not available.
	 *
	 * @return Appropriate LobCreatorBuilder
	 */
	public static LobCreatorBuilderImpl makeLobCreatorBuilder(Dialect dialect) {
		LOB_MESSAGE_LOGGER.disablingContextualLOBCreationSinceConnectionNull();
		return new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(), NONE );
	}

	/**
	 * Build a {@link LobCreator} using the given context
	 *
	 * @param lobCreationContext The LOB creation context
	 *
	 * @return The LobCreator
	 */
	public LobCreator buildLobCreator(LobCreationContext lobCreationContext) {
		if ( supportedContextualLobTypes.isEmpty() ) {
			return NonContextualLobCreator.INSTANCE;
		}
		else if ( supportedContextualLobTypes.contains( LobTypes.BLOB )
				&& supportedContextualLobTypes.contains( LobTypes.CLOB ) ){
			return !supportedContextualLobTypes.contains( LobTypes.NCLOB )
					? new BlobAndClobCreator( lobCreationContext, useConnectionToCreateLob )
					: new StandardLobCreator( lobCreationContext, useConnectionToCreateLob );
		}
		else {
			LOB_LOGGER.debug( "Unexpected condition resolving type of LobCreator to use. Falling back to NonContextualLobCreator" );
			return NonContextualLobCreator.INSTANCE;
		}
	}
}
