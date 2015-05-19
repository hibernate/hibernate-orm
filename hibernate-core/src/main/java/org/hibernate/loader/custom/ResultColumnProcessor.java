/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/** Processor for each "column" in a custom query result.  May map to more than one physical column in the JDBC ResultSert.
 *
 * @author Steve Ebersole
 */
interface ResultColumnProcessor {
	/**
	 * Perform discovery, if needed.  Typically discovery activities include looking up the column name in the
	 * ResultSet or JDBC type codes.
	 *
	 * @param metadata Delegate for accessing metadata about the JDBC ResultSet
	 * @param types The building List of types
	 * @param aliases The building list of column names/aliases
	 *
	 * @throws SQLException Indicates a problem accessing the JDBC objects
	 * @throws HibernateException Indicates a higher-level problem already categorized by Hibernate
	 */
	public void performDiscovery(JdbcResultMetadata metadata, List<Type> types, List<String> aliases)
			throws SQLException, HibernateException;

	/**
	 * Perform The extraction
	 *
	 * @param data All non-scalar results (handled at a higher level than these processors)
	 * @param resultSet The JDBC result set.
	 * @param session The Hibernate Session
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Indicates a problem accessing the JDBC objects
	 * @throws HibernateException Indicates a higher-level problem already categorized by Hibernate
	 */
	public Object extract(Object[] data, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException;
}
