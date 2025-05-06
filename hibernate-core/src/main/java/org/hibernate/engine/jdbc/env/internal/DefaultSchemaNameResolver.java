/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Default implementation of {@link SchemaNameResolver}
 * using {@link Connection#getSchema()}.
 *
 * @author Steve Ebersole
 */
public class DefaultSchemaNameResolver implements SchemaNameResolver {
	public static final DefaultSchemaNameResolver INSTANCE = new DefaultSchemaNameResolver();

	private DefaultSchemaNameResolver() {
	}

	@Override
	public String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException {
		try {
			return connection.getSchema();
		}
		catch (AbstractMethodError ignore) {
			// jConnect and jTDS report that they "support" schemas, but they don't really
			return null;
		}
	}

}
