package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.util.EnumSet;
import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.spi.LobCreatorBuilder;

import static java.util.Objects.requireNonNull;
import static org.hibernate.engine.jdbc.env.internal.LobCreationHelper.NONE;
import static org.hibernate.engine.jdbc.env.internal.LobCreationHelper.getSupportedContextualLobTypes;
import static org.hibernate.engine.jdbc.env.internal.LobCreationLogging.LOB_MESSAGE_LOGGER;

/**
 * Builds {@link LobCreator} instances based on the capabilities of the environment.
 *
 * @author Steve Ebersole
 */
public class LobCreatorBuilderImpl implements LobCreatorBuilder {
	private final boolean useConnectionToCreateLob;
	private final @Nonnull EnumSet<LobTypes> supportedContextualLobTypes;

	public LobCreatorBuilderImpl(boolean useConnectionToCreateLob, @Nonnull EnumSet<LobTypes> supportedContextualLobTypes) {
		this.useConnectionToCreateLob = useConnectionToCreateLob;
		this.supportedContextualLobTypes = supportedContextualLobTypes;
	}

	// factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The public factory method for obtaining the appropriate LOB creator
	 * (according to given JDBC {@link Connection}).
	 *
	 * @param dialect The {@link Dialect} in use
	 * @param configValues The map of settings
	 * @param jdbcConnection A JDBC {@link Connection} which can be used to
	 *        gauge the drivers level of support for creating LOB references.
	 * @see #makeLobCreatorBuilder(Dialect)
	 */
	@Nonnull
	public static LobCreatorBuilderImpl makeLobCreatorBuilder(
			@Nonnull Dialect dialect,
			@Nonnull Map<String,Object> configValues,
			@Nonnull Connection jdbcConnection) {
		requireNonNull( jdbcConnection, "jdbcConnection" );
		return new LobCreatorBuilderImpl( dialect.getLobSupport().useConnectionToCreateLob(),
				getSupportedContextualLobTypes( dialect, configValues, jdbcConnection ) );
	}

	/**
	 * For use when JDBC {@link Connection} is not available.
	 *
	 * @return Appropriate LobCreatorBuilder
	 */
	@Nonnull
	public static LobCreatorBuilderImpl makeLobCreatorBuilder(@Nonnull Dialect dialect) {
		LOB_MESSAGE_LOGGER.disablingContextualLOBCreationSinceConnectionNull();
		return new LobCreatorBuilderImpl( dialect.getLobSupport().useConnectionToCreateLob(), NONE );
	}

	/**
	 * Build a {@link LobCreator} using the given context
	 *
	 * @param lobCreationContext The LOB creation context
	 *
	 * @return The LobCreator
	 */
	@Nonnull
	public LobCreator buildLobCreator(@Nonnull LobCreationContext lobCreationContext) {
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
			LOB_MESSAGE_LOGGER.fallingBackToNonContextual();
			return NonContextualLobCreator.INSTANCE;
		}
	}
}
