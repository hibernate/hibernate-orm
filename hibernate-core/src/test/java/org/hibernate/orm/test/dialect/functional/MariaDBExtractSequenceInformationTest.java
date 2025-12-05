/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Jan Schatteman
 */
@RequiresDialect(value = MariaDBDialect.class)
@SessionFactory
public class MariaDBExtractSequenceInformationTest {

	private final static String hhh15665SeqName = "HHH-15665-seq";

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inSession( session -> {
			session.doWork(  connection -> {
				try (PreparedStatement ps = connection.prepareStatement( "CREATE SEQUENCE IF NOT EXISTS `" + hhh15665SeqName + "`" )) {
					ps.execute();
				}
			} );
		} );
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inSession( session -> {
			session.doWork(  connection -> {
				try (PreparedStatement ps = connection.prepareStatement( "DROP SEQUENCE IF EXISTS `" + hhh15665SeqName + "`" )) {
					ps.execute();
				}
			} );
		} );
	}

	@Test
	@JiraKey(value = "HHH-15665")
	public void testExtractSequenceInformationForMariaDB() {

		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		final JdbcConnectionAccess bootstrapJdbcConnectionAccess = ssr.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection connection;
		try {
			connection = bootstrapJdbcConnectionAccess.obtainConnection();
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}

		try {
			JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );

			Iterable<SequenceInformation> sequenceInformations = SequenceInformationExtractorMariaDBDatabaseImpl.INSTANCE.extractMetadata(
					new ExtractionContext.EmptyExtractionContext() {
						@Override
						public Connection getJdbcConnection() {
							return connection;
						}

						@Override
						public JdbcEnvironment getJdbcEnvironment() {
							return jdbcEnvironment;
						}
					} );

			Assertions.assertNotNull( sequenceInformations );

			Optional<SequenceInformation> seq = StreamSupport.stream( sequenceInformations.spliterator(), false )
					.filter(
							sequence -> hhh15665SeqName.equals( sequence.getSequenceName()
																		.getSequenceName()
																		.getText() )
					)
					.findFirst();

			Assertions.assertTrue( seq.isPresent(), hhh15665SeqName + " not found" );
		}
		catch (SQLException e) {
			Assertions.fail( "Sequence information for " + hhh15665SeqName + " was not retrieved: " + e.getMessage() );
		}
		finally {
			if ( connection != null ) {
				try {
					bootstrapJdbcConnectionAccess.releaseConnection( connection );
				}
				catch (SQLException e) {
					// Ignore
				}
			}
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
