/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SequenceCollector {

	private static final Logger log = Logger.getLogger(SequenceCollector.class);

	public static SequenceCollector create(ConnectionProvider provider) {
		return new SequenceCollector(provider);
	}

	final private ConnectionProvider provider;

	private SequenceCollector(ConnectionProvider provider) {
		this.provider = provider;
	}

	public Set<String> readSequences(
			SequenceInformationExtractor extractor,
			JdbcEnvironment jdbcEnvironment) {
		Set<String> sequences = new HashSet<>();
		Connection connection = null;
		try {
			connection = provider.getConnection();
			final Connection jdbcConnection = connection;
			for ( var sequence : extractor.extractMetadata( new ExtractionContext.EmptyExtractionContext() {
				@Override
				public JdbcEnvironment getJdbcEnvironment() {
					return jdbcEnvironment;
				}

				@Override
				public Connection getJdbcConnection() {
					return jdbcConnection;
				}
			} ) ) {
				sequences.add( sequence.getSequenceName().getSequenceName().getText().toLowerCase().trim() );
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("Problem while reading sequences", e);
		}
		finally {
			if (connection != null) {
				try {
					provider.closeConnection( connection );
				}
				catch (SQLException e) {
					log.warn( "Problem while closing connection", e );
				}
			}
		}
		return sequences;
	}

}
