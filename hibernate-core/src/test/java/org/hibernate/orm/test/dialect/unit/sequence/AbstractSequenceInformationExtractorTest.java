/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.sequence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractSequenceInformationExtractorTest {

	@Test
	public void testSequenceGenerationExtractor() throws SQLException {
		final Dialect dialect = getDialect();
		assertThat(
				dialect.getSequenceInformationExtractor() == SequenceInformationExtractors.none(),
				is( !expectsSequenceMetadata() )
		);
		if ( expectsSequenceMetadata() ) {
			assertMapping( dialect );
		}
	}

	public abstract Dialect getDialect();

	public abstract boolean expectsSequenceMetadata();

	public abstract String expectedQuerySequencesString();

	@SuppressWarnings("unchecked")
	private void assertMapping(Dialect dialect) throws SQLException {
		final boolean db2 = dialect instanceof DB2zDialect;
		final ResultSet rows = mock( ResultSet.class );
		when( rows.next() ).thenReturn( true, false );
		when( rows.getString( db2 ? "seqname" : "sequencename" ) ).thenReturn( "test_sequence" );
		when( rows.getString( db2 ? "seqschema" : "sequence_schema" ) ).thenReturn( "test_schema" );
		when( rows.getLong( db2 ? "start" : "startvalue" ) ).thenReturn( 1L );
		when( rows.getLong( db2 ? "minvalue" : "minimumvalue" ) ).thenReturn( 2L );
		when( rows.getLong( db2 ? "maxvalue" : "maximumvalue" ) ).thenReturn( 3L );
		when( rows.getLong( "increment" ) ).thenReturn( 4L );

		final ExtractionContext context = mock( ExtractionContext.class );
		final JdbcEnvironment jdbcEnvironment = mock( JdbcEnvironment.class );
		final IdentifierHelper identifierHelper = mock( IdentifierHelper.class );
		when( context.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( jdbcEnvironment.getIdentifierHelper() ).thenReturn( identifierHelper );
		when( identifierHelper.toIdentifier( any() ) )
				.thenAnswer( invocation -> org.hibernate.boot.model.naming.Identifier.toIdentifier(
						invocation.getArgument( 0 )
				) );
		final AtomicReference<String> query = new AtomicReference<>();
		doAnswer( invocation -> {
			query.set( invocation.getArgument( 0 ) );
			return ((ExtractionContext.ResultSetProcessor<List<SequenceInformation>>) invocation.getArgument( 2 ))
					.process( rows );
		} ).when( context ).getQueryResults( any(), any(), any() );

		final List<SequenceInformation> results =
				(List<SequenceInformation>) dialect.getSequenceInformationExtractor().extractMetadata( context );
		assertEquals( expectedQuerySequencesString(), query.get() );
		final SequenceInformation result = results.get( 0 );
		assertEquals( "test_sequence", result.getSequenceName().getSequenceName().getText() );
		assertEquals( "test_schema", result.getSequenceName().getSchemaName().getText() );
		assertEquals( 1L, result.getStartValue() );
		assertEquals( 2L, result.getMinValue() );
		assertEquals( 3L, result.getMaxValue() );
		assertEquals( 4L, result.getIncrementValue() );
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		public Integer id;
	}
}
