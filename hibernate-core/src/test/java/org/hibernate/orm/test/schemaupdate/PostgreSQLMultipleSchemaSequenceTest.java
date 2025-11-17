/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.List;

import static org.hibernate.cfg.JdbcSettings.URL;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
public class PostgreSQLMultipleSchemaSequenceTest {
	private static final String extraSchemaName = "extra_schema_sequence_validation";

	@Test
	@JiraKey("HHH-5538")
	public void test(@TempDir File tempDir) throws IOException {
		final File output = new File( tempDir, "update_script.sql" );

		// roughly:
		//		1) Using "main" connection provider -
		//			a) Export (drop & create) the model to both the database and script file
		//			b) Create a schema named $extraSchemaName
		//			c) Verify the initial value of the SEQ_TEST sequence
		//		2) Using a separate connection connecting to the $extraSchemaName schema -
		//			a) Export (drop & create) the model to both the database and script file
		//			b) Verify the initial value of
		//			the SEQ_TEST sequence
		//			c) Verify we had 2 "create sequence" commands
		//
		// ¯_(ツ)_/¯

		// 1
		try (var registry1 = primaryServiceRegistry()) {
			final var metadata1 = new MetadataSources( registry1 )
					.addAnnotatedClass( Box.class )
					.buildMetadata();
			try {
				// 1.a
				new SchemaExport()
						.setOutputFile( output.getAbsolutePath() )
						.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata1 );

				// 1.b
				final ConnectionProvider connectionProvider1 = registry1.requireService( ConnectionProvider.class );
				DdlTransactionIsolatorTestingImpl ddlTransactionIsolator1 = new DdlTransactionIsolatorTestingImpl(
						registry1,
						new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess( connectionProvider1 )
				);
				try(Statement statement = ddlTransactionIsolator1.getIsolatedConnection().createStatement()) {
					statement.execute( String.format( "DROP SCHEMA IF EXISTS %s CASCADE", extraSchemaName ) );
					statement.execute( String.format( "CREATE SCHEMA %s;", extraSchemaName ) );

					// 1.c
					try(ResultSet resultSet = statement.executeQuery( "SELECT NEXTVAL('SEQ_TEST')" )) {
						while ( resultSet.next() ) {
							Long sequenceValue = resultSet.getLong( 1 );
							Assertions.assertEquals( Long.valueOf( 1L ), sequenceValue );
						}
					}
				}
				catch (SQLException e) {
					Assertions.fail( e.getMessage() );
				}

				// 2
				try (var ssr2 = secondaryServiceRegistry()) {
					final MetadataImplementor metadata2 = (MetadataImplementor) new MetadataSources( ssr2 )
							.addAnnotatedClass( Box.class )
							.buildMetadata();

					try {
						// 2.a
						new SchemaExport()
								.setOutputFile( output.getAbsolutePath() )
								.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata2 );

						var connectionProvider2 = ssr2.requireService( ConnectionProvider.class );
						var ddlTransactionIsolator2 = new DdlTransactionIsolatorTestingImpl(
								ssr2,
								new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess( connectionProvider2 )
						);
						try(var statement = ddlTransactionIsolator2.getIsolatedConnection().createStatement()) {
							try(var resultSet = statement.executeQuery( "SELECT NEXTVAL('SEQ_TEST')" )) {
								while ( resultSet.next() ) {
									var sequenceValue = resultSet.getLong( 1 );
									Assertions.assertEquals( Long.valueOf( 1L ), sequenceValue );
								}
							}

							statement.execute( String.format( "DROP SCHEMA IF EXISTS %s CASCADE", extraSchemaName ) );
						}
						catch (SQLException e) {
							Assertions.fail( e.getMessage() );
						}
					}
					finally {
						new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata2 );
					}
				}
			}
			finally {
				new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata1 );
			}

			final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
			Assertions.assertEquals( 2, sqlLines
					.stream()
					.filter( s -> s.equalsIgnoreCase( "create sequence SEQ_TEST start with 1 increment by 1;" ) )
					.count() );
		}
	}

	private ServiceRegistry primaryServiceRegistry() {
		return ServiceRegistryUtil.serviceRegistry();
	}

	private ServiceRegistry secondaryServiceRegistry() {
		String existingUrl = (String) Environment.getProperties().get( URL );
		if ( existingUrl.indexOf( '?' ) == -1 ) {
			existingUrl += "?";
		}
		else {
			existingUrl += "&";
		}
		return ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( URL, existingUrl + "currentSchema=" + extraSchemaName )
				.build();
	}

	@Entity(name = "Box")
	@Table(name = "Box")
	public static class Box {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TEST")
		@SequenceGenerator(name = "TEST", sequenceName = "SEQ_TEST", allocationSize=1)
		public Integer id;

	}
}
