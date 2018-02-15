/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;

/**
 * Contract for resolving the schema of a {@link java.sql.Connection}.
 *
 * @author Steve Ebersole
 */
public interface SchemaNameResolver {
	/**
	 * Given a JDBC {@link java.sql.Connection}, resolve the name of the schema (if one) to which it connects.
	 *
	 * @param connection The JDBC connection
	 * @param dialect The Dialect
	 *
	 * @return The name of the schema (may be null).
	 */
	String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException;
}
