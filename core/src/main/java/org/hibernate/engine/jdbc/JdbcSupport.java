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
 * Isolates Hibernate interactions with JDBC in terms of variations between JDBC 3 (JDK 1.4 and 1.5)
 * and JDBC 4 (JDK 1.6).
 *
 * @author Steve Ebersole
 */
public interface JdbcSupport {
	/**
	 * Creates an instance of a {@link LobCreator} that does not use the underlying JDBC {@link java.sql.Connection}
	 * to create LOBs.
	 * <p/>
	 * This method is here solely to support the older, now-deprecated method of creating LOBs via
	 * the various {@link org.hibernate.Hibernate#createBlob} and {@link org.hibernate.Hibernate#createClob} methods on
	 * {@link org.hibernate.Hibernate}.
	 *
	 * @return The LOB creator.
	 * @deprecated Use {@link #getLobCreator(LobCreationContext)} instead.
	 */
	public LobCreator getLobCreator();

	/**
	 * Create an instance of a {@link LobCreator} appropriate for the current envionment, mainly meant to account for
	 * variance between JDBC 4 (<= JDK 1.6) and JDBC3 (>= JDK 1.5).
	 *
	 * @param lobCreationContext The context in which the LOB is being created
	 * @return The LOB creator.
	 */
	public LobCreator getLobCreator(LobCreationContext lobCreationContext);

	/**
	 * Wrap the given {@link ResultSet} in one that caches the column-name -> column-index resolution.
	 *
	 * @param resultSet The {@link ResultSet} to wrap.
	 * @param columnNameCache The resolution cache.
	 * @return The wrapper.
	 */
	public ResultSet wrap(ResultSet resultSet, ColumnNameCache columnNameCache);
}
