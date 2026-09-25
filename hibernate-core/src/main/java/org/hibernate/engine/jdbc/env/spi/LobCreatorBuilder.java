package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;

/**
 * @author Steve Ebersole
 */
public interface LobCreatorBuilder {
	LobCreator buildLobCreator(LobCreationContext lobCreationContext);
}
