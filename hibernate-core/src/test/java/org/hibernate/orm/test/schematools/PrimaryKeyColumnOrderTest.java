/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schematools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@RequiresDialect(OracleDialect.class)
@DomainModel(annotatedClasses = PrimaryKeyColumnOrderTest.TestEntity.class)
@SessionFactory(exportSchema = false)
public class PrimaryKeyColumnOrderTest extends BaseSessionFactoryFunctionalTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		//noinspection deprecation
		scope.inTransaction(session ->
			session.createNativeQuery(
							"create table TEST_ENTITY ( Z int , A int NOT NULL , B int NOT NULL , CONSTRAINT PK_TEST_ENTITY PRIMARY KEY ( B, A ))" )
					.executeUpdate()
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void getPrimaryKey() throws Exception {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadataAccessStrategy.GROUPED
				)
				.build();
		DdlTransactionIsolator ddlTransactionIsolator = null;
		ExtractionContextImpl extractionContext = null;
		try {
			ddlTransactionIsolator = buildDdlTransactionIsolator( ssr );
			extractionContext = buildContext( ssr, ddlTransactionIsolator );
			TableInformation table = buildInformationExtractor( extractionContext ).getTable(
					null,
					null,
					new Identifier( "TEST_ENTITY", false )
			);
			PrimaryKeyInformation primaryKey = table.getPrimaryKey();
			assertThat( primaryKey ).isNotNull();

			List<String> pkColumnNames = new ArrayList<>();
			primaryKey.getColumns().forEach( columnInformation -> {
				pkColumnNames.add( columnInformation.getColumnIdentifier()
										.getCanonicalName()
										.toLowerCase( Locale.ROOT ) );
			} );

			assertThat( pkColumnNames.size() ).isEqualTo( 2 );
			assertTrue( pkColumnNames.contains( "a" ) );
			assertTrue( pkColumnNames.contains( "b" ) );
		}
		finally {
			if ( extractionContext != null ) {
				extractionContext.cleanup();
			}
			if ( ddlTransactionIsolator != null ) {
				ddlTransactionIsolator.release();
			}
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private InformationExtractor buildInformationExtractor(ExtractionContextImpl extractionContext) throws Exception {
		ExtractionTool extractionTool = new HibernateSchemaManagementTool().getExtractionTool();

		return extractionTool.createInformationExtractor( extractionContext );
	}

	private static ExtractionContextImpl buildContext(
			StandardServiceRegistry ssr,
			DdlTransactionIsolator ddlTransactionIsolator) throws Exception {
		Database database = new MetadataSources( ssr ).buildMetadata().getDatabase();

		SqlStringGenerationContext sqlStringGenerationContext = SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );

		DatabaseInformation dbInfo = buildDatabaseInformation(
				ssr,
				database,
				sqlStringGenerationContext,
				ddlTransactionIsolator
		);

		return new ExtractionContextImpl(
				ssr,
				database.getJdbcEnvironment(),
				sqlStringGenerationContext,
				ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess(),
				(ExtractionContext.DatabaseObjectAccess) dbInfo
		);
	}

	private static DatabaseInformationImpl buildDatabaseInformation(
			StandardServiceRegistry ssr,
			Database database,
			SqlStringGenerationContext sqlStringGenerationContext,
			DdlTransactionIsolator ddlTransactionIsolator) throws Exception {
		return new DatabaseInformationImpl(
				ssr,
				database.getJdbcEnvironment(),
				sqlStringGenerationContext,
				ddlTransactionIsolator,
				database.getServiceRegistry().getService( SchemaManagementTool.class )
		);
	}

	private static DdlTransactionIsolator buildDdlTransactionIsolator(StandardServiceRegistry ssr) {
		final ConnectionProvider connectionProvider = ssr.getService( ConnectionProvider.class );
		return new DdlTransactionIsolatorTestingImpl(
				ssr,
				new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess( connectionProvider )
		);
	}

	@Entity
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@EmbeddedId
		private EntityId id;
	}

	@Embeddable
	public static class EntityId implements Serializable {
		private int A;
		private int B;
	}
}
