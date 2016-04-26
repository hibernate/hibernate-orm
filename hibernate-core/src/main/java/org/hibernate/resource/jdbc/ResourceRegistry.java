/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * A registry for tracking JDBC resources
 *
 * @author Steve Ebersole
 */
public interface ResourceRegistry {
	/**
	 * Does this registry currently have any registered resources?
	 *
	 * @return True if the registry does have registered resources; false otherwise.
	 */
	boolean hasRegisteredResources();

	void releaseResources();

	/**
	 * Register a JDBC statement.
	 *
	 * @param statement The statement to register.
	 * @param cancelable Is the statement being registered capable of being cancelled?  In other words,
	 * should we register it to be the target of subsequent {@link #cancelLastQuery()} calls?
	 */
	void register(Statement statement, boolean cancelable);

	/**
	 * Release a previously registered statement.
	 *
	 * @param statement The statement to release.
	 */
	void release(Statement statement);

	/**
	 * Register a JDBC result set.
	 * <p/>
	 * Implementation note: Second parameter has been introduced to prevent
	 * multiple registrations of the same statement in case {@link java.sql.ResultSet#getStatement()}
	 * does not return original {@link java.sql.Statement} object.
	 *
	 * @param resultSet The result set to register.
	 * @param statement Statement from which {@link java.sql.ResultSet} has been generated.
	 */
	void register(ResultSet resultSet, Statement statement);

	/**
	 * Release a previously registered result set.
	 *
	 * @param resultSet The result set to release.
	 * @param statement Statement from which {@link java.sql.ResultSet} has been generated.
	 */
	void release(ResultSet resultSet, Statement statement);

	void register(Blob blob);
	void release(Blob blob);

	void register(Clob clob);
	void release(Clob clob);

	void register(NClob nclob);
	void release(NClob nclob);

	void cancelLastQuery();

}
