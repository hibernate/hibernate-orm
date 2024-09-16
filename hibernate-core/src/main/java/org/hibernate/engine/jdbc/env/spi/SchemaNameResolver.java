/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;

/**
 * Contract for resolving the schema of a {@link Connection}.
 *
 * @author Steve Ebersole
 */
public interface SchemaNameResolver {
	/**
	 * Given a JDBC {@link Connection}, resolve the name of the schema (if one) to which it connects.
	 *
	 * @param connection The JDBC connection
	 * @param dialect The {@link Dialect}
	 *
	 * @return The name of the schema (may be null).
	 */
	String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException;
}
