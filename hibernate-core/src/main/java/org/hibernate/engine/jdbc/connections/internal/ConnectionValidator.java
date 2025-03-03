/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for validating JDBC {@linkplain Connection connections}.
 *
 * @author Christian Beikov
 */
public interface ConnectionValidator {

	ConnectionValidator ALWAYS_VALID = connection -> true;

	/**
	 * Checks if the given connection is still valid.
	 *
	 * @return {@code true} if the connection is valid, {@code false} otherwise
	 * @throws SQLException when an error happens due to the connection usage leading to a connection close
	 */
	boolean isValid(Connection connection) throws SQLException;
}
