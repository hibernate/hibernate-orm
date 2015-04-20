/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
	public boolean hasRegisteredResources();

	public void releaseResources();

	/**
	 * Register a JDBC statement.
	 *
	 * @param statement The statement to register.
	 * @param cancelable Is the statement being registered capable of being cancelled?  In other words,
	 * should we register it to be the target of subsequent {@link #cancelLastQuery()} calls?
	 */
	public void register(Statement statement, boolean cancelable);

	/**
	 * Release a previously registered statement.
	 *
	 * @param statement The statement to release.
	 */
	public void release(Statement statement);

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
	public void register(ResultSet resultSet, Statement statement);

	/**
	 * Release a previously registered result set.
	 *
	 * @param resultSet The result set to release.
	 * @param statement Statement from which {@link java.sql.ResultSet} has been generated.
	 */
	public void release(ResultSet resultSet, Statement statement);

	public void register(Blob blob);
	public void release(Blob blob);

	public void register(Clob clob);
	public void release(Clob clob);

	public void register(NClob nclob);
	public void release(NClob nclob);

	public void cancelLastQuery();

}
