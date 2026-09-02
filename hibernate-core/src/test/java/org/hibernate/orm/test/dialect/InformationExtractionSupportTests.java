/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyMetadataPolicy;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.tool.schema.extract.spi.TableInformation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies supported information-extractor construction and foreign-key policy.
///
/// @author Steve Ebersole
public class InformationExtractionSupportTests {
	@Test
	void policyRejectsImpossibleCombinations() {
		assertThat( ForeignKeyMetadataPolicy.importedKeysOnly() )
				.isSameAs( ForeignKeyMetadataPolicy.importedKeysOnly() );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new ForeignKeyMetadataPolicy( null, null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new ForeignKeyMetadataPolicy(
						ForeignKeyMetadataPolicy.Mode.IMPORTED_KEYS_ONLY,
						"%"
				) );
	}

	@Test
	void facadeRejectsMissingConstructionInputs() {
		assertThatIllegalArgumentException()
				.isThrownBy( () -> InformationExtractors.jdbcMetadata( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> InformationExtractors.jdbcMetadata( extractionContext(), null ) );
	}

	@Test
	void facadeReturnsFreshContextBoundStockProfiles() {
		final ExtractionContext context = extractionContext();
		assertThat( InformationExtractors.jdbcMetadata( context ) )
				.isNotSameAs( InformationExtractors.jdbcMetadata( context ) )
				.hasSameClassAs( InformationExtractors.jdbcMetadata( extractionContext() ) );
		assertThat( InformationExtractors.mysql( context ).getClass().getSimpleName() )
				.isEqualTo( "InformationExtractorMySQLImpl" );
		assertThat( InformationExtractors.postgresql( context ).getClass().getSimpleName() )
				.isEqualTo( "InformationExtractorPostgreSQLImpl" );
		assertThat( InformationExtractors.oracle( context ).getClass().getSimpleName() )
				.isEqualTo( "InformationExtractorOracleImpl" );
	}

	@Test
	void crossReferenceFactoryPreservesEveryJdbcFilterMeaning() {
		assertThat( ForeignKeyMetadataPolicy.importedKeysAndCrossReference( null )
				.crossReferenceParentTableFilter() ).isNull();
		assertThat( ForeignKeyMetadataPolicy.importedKeysAndCrossReference( "" )
				.crossReferenceParentTableFilter() ).isEmpty();
		assertThat( ForeignKeyMetadataPolicy.importedKeysAndCrossReference( "%" )
				.crossReferenceParentTableFilter() ).isEqualTo( "%" );
		assertThat( ForeignKeyMetadataPolicy.importedKeysAndCrossReference( "parent" )
				.crossReferenceParentTableFilter() ).isEqualTo( "parent" );
	}

	@Test
	void importedKeysOnlyDoesNotInvokeCrossReference() {
		final RecordingInformationExtractor extractor = new RecordingInformationExtractor(
				extractionContext(),
				ForeignKeyMetadataPolicy.importedKeysOnly()
		);

		extractor.getForeignKeys( tableInformation() );

		assertThat( extractor.importedKeysArguments ).containsExactly( "catalog", "schema", "child" );
		assertThat( extractor.crossReferenceArguments ).isNull();
	}

	@Test
	void crossReferencePolicyUsesTheExactParentFilterAndChildCoordinates() {
		final RecordingInformationExtractor extractor = new RecordingInformationExtractor(
				extractionContext(),
				ForeignKeyMetadataPolicy.importedKeysAndCrossReference( "%" )
		);

		extractor.getForeignKeys( tableInformation() );

		assertThat( extractor.importedKeysArguments ).containsExactly( "catalog", "schema", "child" );
		assertThat( extractor.crossReferenceArguments )
				.containsExactly( null, null, "%", "catalog", "schema", "child" );
	}

	private static ExtractionContext extractionContext() {
		final ConfigurationService configurationService = new ConfigurationService() {
			@Override
			public Map<String, Object> getSettings() {
				return Map.of();
			}

			@Override
			public <T> T getSetting(String name, Converter<T> converter) {
				return null;
			}

			@Override
			public <T> T getSetting(String name, Converter<T> converter, T defaultValue) {
				return defaultValue;
			}

			@Override
			public <T> T getSetting(String name, Class<T> expected, T defaultValue) {
				return defaultValue;
			}
		};
		final ServiceRegistry serviceRegistry = mock( ServiceRegistry.class );
		when( serviceRegistry.requireService( ConfigurationService.class ) ).thenReturn( configurationService );
		final JdbcEnvironment jdbcEnvironment = mock( JdbcEnvironment.class );
		when( jdbcEnvironment.getDialect() ).thenReturn( new H2Dialect() );
		final ExtractionContext.DatabaseObjectAccess databaseObjectAccess =
				mock( ExtractionContext.DatabaseObjectAccess.class );
		when( databaseObjectAccess.isCaching() ).thenReturn( false );

		final ExtractionContext context = mock( ExtractionContext.class );
		when( context.getServiceRegistry() ).thenReturn( serviceRegistry );
		when( context.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( context.getDatabaseObjectAccess() ).thenReturn( databaseObjectAccess );
		return context;
	}

	private static TableInformation tableInformation() {
		final TableInformation tableInformation = mock( TableInformation.class );
		when( tableInformation.getName() ).thenReturn(
				new QualifiedTableName(
						Identifier.toIdentifier( "catalog" ),
						Identifier.toIdentifier( "schema" ),
						Identifier.toIdentifier( "child" )
				)
		);
		return tableInformation;
	}

	private static class RecordingInformationExtractor extends InformationExtractorJdbcDatabaseMetaDataImpl {
		private List<String> importedKeysArguments;
		private List<String> crossReferenceArguments;

		private RecordingInformationExtractor(
				ExtractionContext extractionContext,
				ForeignKeyMetadataPolicy foreignKeyMetadataPolicy) {
			super( extractionContext, foreignKeyMetadataPolicy );
		}

		@Override
		protected <T> T processImportedKeysResultSet(
				String catalog,
				String schema,
				String table,
				ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
			importedKeysArguments = List.of( catalog, schema, table );
			return processor.process( mock( ResultSet.class ) );
		}

		@Override
		protected <T> T processCrossReferenceResultSet(
				String parentCatalog,
				String parentSchema,
				String parentTable,
				String foreignCatalog,
				String foreignSchema,
				String foreignTable,
				ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
			crossReferenceArguments = java.util.Arrays.asList(
					parentCatalog,
					parentSchema,
					parentTable,
					foreignCatalog,
					foreignSchema,
					foreignTable
			);
			return processor.process( mock( ResultSet.class ) );
		}
	}
}
