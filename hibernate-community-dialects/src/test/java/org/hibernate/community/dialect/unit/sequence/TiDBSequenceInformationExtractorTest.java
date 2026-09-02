/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.unit.sequence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the custom two-stage TiDB sequence metadata extractor.
///
/// @author Steve Ebersole
public class TiDBSequenceInformationExtractorTest {
	@Test
	@SuppressWarnings("unchecked")
	void extractsDiscoveredSequences() throws Exception {
		final ExtractionContext context = mock( ExtractionContext.class );
		final Connection connection = mock( Connection.class );
		final Statement namesStatement = mock( Statement.class );
		final Statement metadataStatement = mock( Statement.class );
		final ResultSet names = mock( ResultSet.class );
		final ResultSet metadata = mock( ResultSet.class );
		final JdbcEnvironment jdbcEnvironment = mock( JdbcEnvironment.class );
		final IdentifierHelper identifierHelper = mock( IdentifierHelper.class );
		when( context.getJdbcConnection() ).thenReturn( connection );
		when( context.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( jdbcEnvironment.getIdentifierHelper() ).thenReturn( identifierHelper );
		when( identifierHelper.toIdentifier( any() ) )
				.thenAnswer( invocation -> Identifier.toIdentifier( invocation.getArgument( 0 ) ) );
		when( connection.createStatement() ).thenReturn( namesStatement, metadataStatement );
		when( namesStatement.executeQuery(
				"SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = database()"
		) ).thenReturn( names );
		when( names.next() ).thenReturn( true, true, false );
		when( names.getString( "sequence_name" ) ).thenReturn( "first_sequence", "second_sequence" );
		when( metadataStatement.executeQuery( any() ) ).thenReturn( metadata );
		when( metadata.next() ).thenReturn( true, false );
		when( metadata.getString( "sequence_name" ) ).thenReturn( "first_sequence" );
		when( metadata.getLong( "start_value" ) ).thenReturn( 10L );
		when( metadata.getLong( "minimum_value" ) ).thenReturn( 1L );
		when( metadata.getLong( "maximum_value" ) ).thenReturn( 100L );
		when( metadata.getLong( "increment" ) ).thenReturn( 5L );

		final List<SequenceInformation> results = (List<SequenceInformation>) new TiDBDialect()
				.getSequenceInformationExtractor()
				.extractMetadata( context );

		assertThat( results ).hasSize( 1 );
		assertThat( results.get( 0 ).getSequenceName().getSequenceName().getText() )
				.isEqualTo( "first_sequence" );
		assertThat( results.get( 0 ).getStartValue() ).isEqualTo( 10L );
		assertThat( results.get( 0 ).getIncrementValue() ).isEqualTo( 5L );
		verify( metadataStatement ).executeQuery( org.mockito.ArgumentMatchers.argThat(
				query -> query.contains( "first_sequence" )
						&& query.contains( "UNION ALL" )
						&& query.contains( "second_sequence" )
		) );
	}
}
