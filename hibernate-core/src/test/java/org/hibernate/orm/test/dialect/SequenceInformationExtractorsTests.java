/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the supported sequence-metadata extraction facility independently
/// of any database-specific provider.
///
/// @author Steve Ebersole
public class SequenceInformationExtractorsTests {
	@Test
	void defaultAndColumnMappingsPreserveRowOrder() throws SQLException {
		final ResultSet rows = mock( ResultSet.class );
		when( rows.next() ).thenReturn( true, true, false );
		when( rows.getString( "name_col" ) ).thenReturn( "first", "second" );
		when( rows.getString( "catalog_col" ) ).thenReturn( "catalog1", "catalog2" );
		when( rows.getString( "schema_col" ) ).thenReturn( "schema1", "schema2" );
		when( rows.getLong( "start_col" ) ).thenReturn( 1L, 2L );
		when( rows.getLong( "min_col" ) ).thenReturn( 3L, 4L );
		when( rows.getLong( "max_col" ) ).thenReturn( 5L, 6L );
		when( rows.getLong( "increment_col" ) ).thenReturn( 7L, 8L );

		final var extractor = SequenceInformationExtractors.builder( "lookup sequences" )
				.sequenceNameColumn( "name_col" )
				.catalogColumn( "catalog_col" )
				.schemaColumn( "schema_col" )
				.startValueColumn( "start_col" )
				.minimumValueColumn( "min_col" )
				.maximumValueColumn( "max_col" )
				.incrementValueColumn( "increment_col" )
				.build();
		final List<SequenceInformation> results = extract( extractor, "lookup sequences", rows );

		assertThat( results ).extracting( result -> result.getSequenceName().getSequenceName().getText() )
				.containsExactly( "first", "second" );
		assertThat( results.get( 0 ).getSequenceName().getCatalogName().getText() ).isEqualTo( "catalog1" );
		assertThat( results.get( 0 ).getSequenceName().getSchemaName().getText() ).isEqualTo( "schema1" );
		assertThat( results.get( 0 ).getStartValue() ).isEqualTo( 1L );
		assertThat( results.get( 0 ).getMinValue() ).isEqualTo( 3L );
		assertThat( results.get( 0 ).getMaxValue() ).isEqualTo( 5L );
		assertThat( results.get( 0 ).getIncrementValue() ).isEqualTo( 7L );
	}

	@Test
	void defaultsUseStandardLabels() throws SQLException {
		final ResultSet rows = mock( ResultSet.class );
		when( rows.next() ).thenReturn( true, false );
		when( rows.getString( "sequence_name" ) ).thenReturn( "standard" );
		when( rows.getString( "sequence_catalog" ) ).thenReturn( "catalog" );
		when( rows.getString( "sequence_schema" ) ).thenReturn( "schema" );
		when( rows.getLong( "start_value" ) ).thenReturn( 11L );
		when( rows.getLong( "minimum_value" ) ).thenReturn( 12L );
		when( rows.getLong( "maximum_value" ) ).thenReturn( 13L );
		when( rows.getLong( "increment" ) ).thenReturn( 14L );

		final SequenceInformation result = extract(
				SequenceInformationExtractors.builder( "standard lookup" ).build(),
				"standard lookup",
				rows
		).get( 0 );
		assertThat( result.getStartValue() ).isEqualTo( 11L );
		assertThat( result.getMinValue() ).isEqualTo( 12L );
		assertThat( result.getMaxValue() ).isEqualTo( 13L );
		assertThat( result.getIncrementValue() ).isEqualTo( 14L );
	}

	@Test
	void positionalNameAbsenceAndCustomReaders() throws SQLException {
		final ResultSet rows = mock( ResultSet.class );
		when( rows.next() ).thenReturn( true, false );
		when( rows.getString( 1 ) ).thenReturn( "positioned" );
		when( rows.getString( "custom_schema" ) ).thenReturn( "schema" );
		when( rows.getBigDecimal( "decimal_increment" ) ).thenReturn( new java.math.BigDecimal( "2.5" ) );

		final SequenceInformation result = extract(
				SequenceInformationExtractors.builder( "custom lookup" )
						.sequenceNameColumn( 1 )
						.withoutCatalog()
						.schemaReader( row -> row.getString( "custom_schema" ).toUpperCase() )
						.withoutStartValue()
						.withoutMinimumValue()
						.withoutMaximumValue()
						.incrementValueReader( row -> row.getBigDecimal( "decimal_increment" ) )
						.build(),
				"custom lookup",
				rows
		).get( 0 );

		assertThat( result.getSequenceName().getCatalogName() ).isNull();
		assertThat( result.getSequenceName().getSchemaName().getText() ).isEqualTo( "SCHEMA" );
		assertThat( result.getStartValue() ).isNull();
		assertThat( result.getMinValue() ).isNull();
		assertThat( result.getMaxValue() ).isNull();
		assertThat( result.getIncrementValue() ).isEqualTo( new java.math.BigDecimal( "2.5" ) );
	}

	@Test
	void oracleMappingPreservesDecimalValues() throws SQLException {
		final ResultSet rows = mock( ResultSet.class );
		when( rows.next() ).thenReturn( true, false );
		when( rows.getString( "sequence_name" ) ).thenReturn( "oracle_sequence" );
		when( rows.getBigDecimal( "min_value" ) ).thenReturn( new java.math.BigDecimal( "-999999999999.5" ) );
		when( rows.getBigDecimal( "max_value" ) ).thenReturn( new java.math.BigDecimal( "999999999999.5" ) );
		when( rows.getBigDecimal( "increment_by" ) ).thenReturn( new java.math.BigDecimal( "2.5" ) );

		final SequenceInformation result = extract(
				new OracleDialect().getSequenceInformationExtractor(),
				"select * from all_sequences",
				rows
		).get( 0 );
		assertThat( result.getSequenceName().getCatalogName() ).isNull();
		assertThat( result.getSequenceName().getSchemaName() ).isNull();
		assertThat( result.getStartValue() ).isNull();
		assertThat( result.getMinValue() ).isEqualTo( new java.math.BigDecimal( "-999999999999.5" ) );
		assertThat( result.getMaxValue() ).isEqualTo( new java.math.BigDecimal( "999999999999.5" ) );
		assertThat( result.getIncrementValue() ).isEqualTo( new java.math.BigDecimal( "2.5" ) );
	}

	@Test
	void lastConfigurationWinsAndBuildTakesAnImmutableSnapshot() throws SQLException {
		final var builder = SequenceInformationExtractors.builder( "snapshot lookup" )
				.sequenceNameColumn( "ignored" )
				.sequenceNameReader( row -> "captured" )
				.withoutCatalog()
				.withoutSchema()
				.withoutStartValue()
				.withoutMinimumValue()
				.withoutMaximumValue()
				.withoutIncrementValue();
		final SequenceInformationExtractor captured = builder.build();
		builder.sequenceNameReader( row -> "later" );

		final ResultSet rows = mock( ResultSet.class );
		when( rows.next() ).thenReturn( true, false );
		assertThat( extract( captured, "snapshot lookup", rows ).get( 0 ).getSequenceName()
				.getSequenceName().getText() ).isEqualTo( "captured" );
	}

	@Test
	void emptyNoOpConstructionValidationAndSqlExceptionBehavior() throws SQLException {
		assertThat( SequenceInformationExtractors.none() ).isSameAs( SequenceInformationExtractors.none() );
		final ExtractionContext untouched = mock( ExtractionContext.class );
		assertThat( SequenceInformationExtractors.none().extractMetadata( untouched ) ).isEmpty();
		verify( untouched, never() ).getQueryResults( any(), any(), any() );

		final ResultSet emptyRows = mock( ResultSet.class );
		when( emptyRows.next() ).thenReturn( false );
		assertThat( extract(
				SequenceInformationExtractors.builder( "empty lookup" ).build(),
				"empty lookup",
				emptyRows
		) ).isEmpty();

		assertThatIllegalArgumentException().isThrownBy( () -> SequenceInformationExtractors.builder( " " ) );
		assertThatIllegalArgumentException().isThrownBy( () -> SequenceInformationExtractors.builder( "sql" )
				.sequenceNameColumn( 0 ) );
		assertThatIllegalArgumentException().isThrownBy( () -> SequenceInformationExtractors.builder( "sql" )
				.sequenceNameColumn( "" ) );
		assertThatIllegalArgumentException().isThrownBy( () -> SequenceInformationExtractors.builder( "sql" )
				.catalogReader( null ) );
		assertThatIllegalArgumentException().isThrownBy( () -> SequenceInformationExtractors.information(
				null, null, null, null, null
		) );

		final ExtractionContext failing = mock( ExtractionContext.class );
		when( failing.getQueryResults( eq( "failing lookup" ), eq( null ), any() ) )
				.thenThrow( new SQLException( "expected" ) );
		assertThatThrownBy( () -> SequenceInformationExtractors.builder( "failing lookup" ).build()
				.extractMetadata( failing ) )
				.isInstanceOf( SQLException.class )
				.hasMessage( "expected" );
	}

	@Test
	void constructsSequenceInformation() {
		final QualifiedSequenceName name = new QualifiedSequenceName(
				Identifier.toIdentifier( "catalog" ),
				Identifier.toIdentifier( "schema" ),
				Identifier.toIdentifier( "sequence" )
		);
		final SequenceInformation information = SequenceInformationExtractors.information( name, 1, 2, 3, 4 );
		assertThat( information.getSequenceName() ).isSameAs( name );
	}

	@SuppressWarnings("unchecked")
	private static List<SequenceInformation> extract(
			SequenceInformationExtractor extractor,
			String expectedSql,
			ResultSet rows) throws SQLException {
		final ExtractionContext context = mock( ExtractionContext.class );
		final JdbcEnvironment jdbcEnvironment = mock( JdbcEnvironment.class );
		final IdentifierHelper identifierHelper = mock( IdentifierHelper.class );
		when( context.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( jdbcEnvironment.getIdentifierHelper() ).thenReturn( identifierHelper );
		when( identifierHelper.toIdentifier( any() ) )
				.thenAnswer( invocation -> Identifier.toIdentifier( invocation.getArgument( 0 ) ) );
		final AtomicReference<String> sql = new AtomicReference<>();
		doAnswer( invocation -> {
			sql.set( invocation.getArgument( 0 ) );
			return ((ExtractionContext.ResultSetProcessor<List<SequenceInformation>>) invocation.getArgument( 2 ))
					.process( rows );
		} ).when( context ).getQueryResults( any(), any(), any() );

		final List<SequenceInformation> results = (List<SequenceInformation>) extractor.extractMetadata( context );
		assertThat( sql ).hasValue( expectedSql );
		return results;
	}
}
