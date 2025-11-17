/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;


import java.sql.Connection;

/**
 * Contract for resolving the schema of a {@link Connection}.
 *
 * @apiNote No used.
 * @deprecated Use {@linkplain org.hibernate.engine.jdbc.env.spi.SchemaNameResolver} instead.
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "7.0", forRemoval = true)
public interface SchemaNameResolver {
	/**
	/**
	 * Given a JDBC {@link Connection}, resolve the name of the schema (if one) to which it connects.
	 *
	 * @param connection The JDBC connection
	 *
	 * @return The name of the schema; may be null.
	 */
	String resolveSchemaName(Connection connection);
}
