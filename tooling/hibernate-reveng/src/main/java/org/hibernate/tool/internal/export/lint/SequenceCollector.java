/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2020-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.export.lint;

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
	
	final private ConnectionProvider provider;
	
	private SequenceCollector(ConnectionProvider provider) {
		this.provider = provider;
	}

	public Set<String> readSequences(String sql) {
		Set<String> sequences = new HashSet<String>();
		if (sql != null) {
			Connection connection = null;
			try {

				connection = provider.getConnection();
				Statement statement = null;
				ResultSet rs = null;
				try {
					statement = connection.createStatement();
					rs = statement.executeQuery(sql);
					while (rs.next()) {
						sequences.add(rs.getString("SEQUENCE_NAME").toLowerCase().trim());
					}
				} finally {
					if (rs != null)
						rs.close();
					if (statement != null)
						statement.close();
				}

			} catch (SQLException e) {
				throw new RuntimeException("Problem while closing connection", e);
			} finally {
				if (connection != null)
					try {
						provider.closeConnection(connection);
					} catch (SQLException e) {
						throw new RuntimeException("Problem while closing connection", e);
					}
			}
		}
		return sequences;
	}

}
