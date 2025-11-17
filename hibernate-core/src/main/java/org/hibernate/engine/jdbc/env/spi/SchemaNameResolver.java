/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;

/**
 * Contract for resolving the schema of a {@link Connection}.
 *
 * @apiNote Exists mainly for historical reasons when Hibernate
 * baselines on Java versions before 8 when {@linkplain Connection#getSchema()}
 * was introduced.  We still use it at the moment because some drivers do not
 * implement it (jTDS) and/or some databases do not support schemas and
 * their drivers don't DoTheRightThing.
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
	 * @return The name of the schema; may be null.
	 */
	String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException;
}
