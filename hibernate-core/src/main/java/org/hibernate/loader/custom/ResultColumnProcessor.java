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
