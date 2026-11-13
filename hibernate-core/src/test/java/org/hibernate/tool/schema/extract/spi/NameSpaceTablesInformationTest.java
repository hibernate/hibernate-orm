/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
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
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RequiresDialect(H2Dialect.class)
@RequiresDialect(OracleDialect.class)
@DomainModel(
		annotatedClasses = {
				NameSpaceTablesInformationTest.TestEntity.class
		}
)
@SessionFactory(
		exportSchema = false
)

public class NameSpaceTablesInformationTest extends BaseSessionFactoryFunctionalTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createNativeQuery(
										"create table TEST_TABLE ( Field1 int, Field2 int NOT NULL, Field3 int NOT NULL)" )
								.executeUpdate()
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createNativeQuery( "drop table TEST_TABLE" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-14270")
	public void testNameSpaceTablesInformation() throws Exception {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadaAccessStrategy.GROUPED
				)
				.build();
		DdlTransactionIsolator ddlTransactionIsolator = null;
		ExtractionContextImpl extractionContext = null;

		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( TestEntity.class );
		final Metadata metadata = metadataSources.buildMetadata();
		final NameSpaceTablesInformation tablesInformation = new NameSpaceTablesInformation( metadata.getDatabase().getJdbcEnvironment().getIdentifierHelper() );

		try {
			ddlTransactionIsolator = buildDdlTransactionIsolator( ssr );
			extractionContext = buildContext( ssr, ddlTransactionIsolator );
			TableInformation tableInformation = buildInformationExtractor( extractionContext ).getTable(
					null,
					null,
					new Identifier( "TEST_TABLE", false )
			);
			tablesInformation.addTableInformation( tableInformation );
			Iterable<Namespace> namespaces = metadata.getDatabase().getNamespaces();

			boolean testTableFound = false;
			for ( Namespace namespace : namespaces ) {
				for ( Table table : namespace.getTables() ) {
					final TableInformation tableInfo = tablesInformation.getTableInformation( table );
					if ( tableInfo != null ) {
						testTableFound = Objects.equals( tableInfo.getName().getTableName().getText(), "TEST_TABLE" );
					}
				}
			}

			assertThat("TEST_TABLE is existing in DB.",
					testTableFound,
					is(true));
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
	@jakarta.persistence.Table(name = "TEST_TABLE")
	public static class TestEntity {

		@Id
		private Long id;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}
}
