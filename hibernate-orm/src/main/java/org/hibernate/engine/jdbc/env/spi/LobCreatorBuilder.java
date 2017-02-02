/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;

/**
 * @author Steve Ebersole
 */
public interface LobCreatorBuilder {
	LobCreator buildLobCreator(LobCreationContext lobCreationContext);
}
