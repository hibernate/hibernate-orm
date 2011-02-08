/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.spi;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Defines a registry of JDBC resources related to a particular unit of work.
 *
 * @author Steve Ebersole
 */
public interface JdbcResourceRegistry {
	/**
	 * Register a JDBC statement.
	 *
	 * @param statement The statement to register.
	 */
	public void register(Statement statement);

	public void registerLastQuery(Statement statement);

	public void cancelLastQuery();
	
	/**
	 * Release a previously registered statement.
	 *
	 * @param statement The statement to release.
	 */
	public void release(Statement statement);

	/**
	 * Register a JDBC result set.
	 *
	 * @param resultSet The result set to register.
	 */
	public void register(ResultSet resultSet);

	/**
	 * Release a previously registered result set.
	 *
	 * @param resultSet The result set to release.
	 */
	public void release(ResultSet resultSet);

	/**
	 * Does this registry currently have any registered resources?
	 *
	 * @return True if the registry does have registered resources; false otherwise.
	 */
	public boolean hasRegisteredResources();

	/**
	 * Release all registered resources.
	 */
	public void releaseResources();

	/**
	 * Close this registry.  Also {@link #releaseResources releases} any registered resources.
	 * <p/>
	 * After execution, the registry is considered unusable.
	 */
	public void close();
}
