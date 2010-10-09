/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc;

import java.sql.ResultSet;

/**
 * Central place for locating JDBC support elements.
 *
 * @author Steve Ebersole
 */
public class JdbcSupport {
	public final boolean userContextualLobCreator;

	/**
	 * Create a support object
	 *
	 * @param userContextualLobCreator Should we use contextual (using the JDBC {@link java.sql.Connection}) to
	 * create LOB instances.  In almost all instances this should be the case.  However if the underlying driver
	 * does not support the {@link java.sql.Connection#createBlob()}, {@link java.sql.Connection#createClob()} or
	 * {@link java.sql.Connection#createNClob()} methods this will need to be set to false.
	 */
	public JdbcSupport(boolean userContextualLobCreator) {
		this.userContextualLobCreator = userContextualLobCreator;
	}

	/**
	 * Get an explcitly non-contextual LOB creator.
	 *
	 * @return The LOB creator
	 */
	public LobCreator getLobCreator() {
		return NonContextualLobCreator.INSTANCE;
	}

	/**
	 * Get a LOB creator, based on the given context
	 *
	 * @return The LOB creator
	 */
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return userContextualLobCreator
				? new ContextualLobCreator( lobCreationContext )
				: NonContextualLobCreator.INSTANCE;
	}

	/**
	 * Wrap a result set in a "colun name cache" wrapper.
	 *
	 * @param resultSet The result set to wrap
	 * @param columnNameCache The column name cache.
	 *
	 * @return The wrapped result set.
	 */
	public ResultSet wrap(ResultSet resultSet, ColumnNameCache columnNameCache) {
		return ResultSetWrapperProxy.generateProxy( resultSet, columnNameCache );
	}
}
