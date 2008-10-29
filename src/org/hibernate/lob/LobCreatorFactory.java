//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.lob;

import org.hibernate.Session;
import org.hibernate.AssertionFailure;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * A factory for creating an instance of the appropriate LobCreator implementation.
 *
 * @author Gail Badner
 */
public class LobCreatorFactory {

	/**
	 * Creates an instance of a LobCreator that does not use the Connection to create LOBs.
	 * The returned LobCreator is appropriate for JVM and/or JDBC drivers that support JDBC3,
	 * but not JDBC4.
	 *
	 * @return the LobCreator
	 */
	public static LobCreator createLobCreator() {
		return new LobCreatorImplJDBC3();
	}

	/**
	 * If the setting for Environment#USE_CONNECTION_FOR_LOB_CREATION is true, then the
	 * returned LobCreator will use the Connection to create LOBs. If it is false, then
	 * the returned LobCreator will not use the Connection to create LOBs.
	 *
	 * If the property is not set, then the returned LobCreator will use the connection to
	 * create LOBs only if the JVM and JDBC driver support this JDBC4 functionality.
	 *
	 * (@see Environment#USE_CONNECTION_FOR_LOB_CREATION)
	 *
	 * @param session The session.
	 * @return the LobCreator
	 */
	public static LobCreator createLobCreator(Session session) {
		if ( session == null ) {
			throw new AssertionFailure("session is null; use LobCreatorFactory.createLobCreator() instead.");
		}
		SessionFactoryImplementor sfi = ( SessionFactoryImplementor ) session.getSessionFactory();
		if ( sfi.getSettings().isUseConnectionForLobCreationEnabled() ) {
			return new LobCreatorImplJDBC4( session );
		}
		else {
			return new LobCreatorImplJDBC3();
		}
	}
}