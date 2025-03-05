/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;

/**
 * @author Jan Schatteman
 */
@RequiresDialect(value = MariaDBDialect.class)
public class MariaDBExtractSequenceInformationTest {

	private final static String hhh15665SeqName = "HHH-15665-seq";

	@BeforeAll
	public static void setUp() throws Exception {
		doInAutoCommit( "CREATE SEQUENCE IF NOT EXISTS `" + hhh15665SeqName + "`" );
	}

	@AfterAll
	public static void tearDown() throws SQLException {
		doInAutoCommit( "DROP SEQUENCE IF EXISTS `" + hhh15665SeqName + "`" );
	}

	@Test
	@JiraKey(value = "HHH-15665")
	public void testExtractSequenceInformationForSqlServerWithCaseSensitiveCollation() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );
			TransactionUtil.doWithJDBC( ssr, connection -> {
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
			} );
		}
		catch (SQLException e) {
			Assertions.fail( "Sequence information for " + hhh15665SeqName + " was not retrieved: " + e.getMessage() );
		}
	}
}
