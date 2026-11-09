package org.hibernate.engine.jdbc.env.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;

/**
 * @author Steve Ebersole
 */
public interface LobCreatorBuilder {
	@Nonnull
	LobCreator buildLobCreator(@Nonnull LobCreationContext lobCreationContext);
}
