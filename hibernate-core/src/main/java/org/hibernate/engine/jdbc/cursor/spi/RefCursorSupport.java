/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc.cursor.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.service.Service;

/**
 * Contract for JDBC REF_CURSOR support.
 *
 * @author Steve Ebersole
 *
 * @since 4.3
 */
public interface RefCursorSupport extends Service {
	/**
	 * Register a parameter capable of returning a {@link java.sql.ResultSet} *by position*.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 */
	public void registerRefCursorParameter(CallableStatement statement, int position);

	/**
	 * Register a parameter capable of returning a {@link java.sql.ResultSet} *by name*.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 */
	public void registerRefCursorParameter(CallableStatement statement, String name);

	/**
	 * Given a callable statement previously processed by {@link #registerRefCursorParameter(java.sql.CallableStatement, int)},
	 * extract the {@link java.sql.ResultSet}.
	 *
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The extracted result set.
	 */
	public ResultSet getResultSet(CallableStatement statement, int position);

	/**
	 * Given a callable statement previously processed by {@link #registerRefCursorParameter(java.sql.CallableStatement, String)},
	 * extract the {@link java.sql.ResultSet}.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The extracted result set.
	 */
	public ResultSet getResultSet(CallableStatement statement, String name);
}
