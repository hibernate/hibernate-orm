/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * Register a parameter capable of returning a {@link ResultSet} *by position*.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 */
	void registerRefCursorParameter(CallableStatement statement, int position);

	/**
	 * Register a parameter capable of returning a {@link ResultSet} *by name*.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 */
	void registerRefCursorParameter(CallableStatement statement, String name);

	/**
	 * Given a callable statement previously processed by {@link #registerRefCursorParameter(CallableStatement, int)},
	 * extract the {@link ResultSet}.
	 *
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The extracted result set.
	 */
	ResultSet getResultSet(CallableStatement statement, int position);

	/**
	 * Given a callable statement previously processed by {@link #registerRefCursorParameter(CallableStatement, String)},
	 * extract the {@link ResultSet}.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The extracted result set.
	 */
	ResultSet getResultSet(CallableStatement statement, String name);
}
