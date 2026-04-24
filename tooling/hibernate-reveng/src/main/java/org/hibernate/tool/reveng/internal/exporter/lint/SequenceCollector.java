/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

public class SequenceCollector {

	public static SequenceCollector create(ConnectionProvider provider) {
		return new SequenceCollector(provider);
	}

	private final ConnectionProvider provider;

	private SequenceCollector(ConnectionProvider provider) {
		this.provider = provider;
	}

	public Set<String> readSequences(String sql) {
		Set<String> sequences = new HashSet<>();
		if (sql != null) {
			Connection connection = null;
			try {
				connection = provider.getConnection();
				try (Statement statement = connection.createStatement();
					ResultSet rs = statement.executeQuery(sql)) {
					while (rs.next()) {
						sequences.add(rs.getString("SEQUENCE_NAME").toLowerCase().trim());
					}
				}
			}
			catch (SQLException e) {
				throw new RuntimeException("Problem while closing connection", e);
			}
			finally {
				if (connection != null) {
					try {
						provider.closeConnection(connection);
					}
					catch (SQLException e) {
						throw new RuntimeException("Problem while closing connection", e);
					}
				}
			}
		}
		return sequences;
	}
}
