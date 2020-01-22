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
