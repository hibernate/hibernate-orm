/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Exercises sequence metadata extraction from a standalone Dialect provider.
///
/// @author Steve Ebersole
public class ExampleSequenceInformationExtractorTest {
	@Test
	void extractsProviderDefinedMapping() throws SQLException {
		final ResultSet rows = fixtureRows();
		final var identifierHelper = IdentifierHelperBuilder.from( null ).build();
		final JdbcEnvironment jdbcEnvironment = (JdbcEnvironment) Proxy.newProxyInstance(
				JdbcEnvironment.class.getClassLoader(),
				new Class<?>[] { JdbcEnvironment.class },
				(proxy, method, arguments) -> method.getName().equals( "getIdentifierHelper" )
						? identifierHelper
						: null
		);
		final AtomicReference<String> query = new AtomicReference<>();
		final ExtractionContext context = new ExtractionContext.EmptyExtractionContext() {
			@Override
			public JdbcEnvironment getJdbcEnvironment() {
				return jdbcEnvironment;
			}

			@Override
			public <T> T getQueryResults(
					String queryString,
					Object[] positionalParameters,
					ResultSetProcessor<T> resultSetProcessor) throws SQLException {
				query.set( queryString );
				return resultSetProcessor.process( rows );
			}
		};

		final List<SequenceInformation> results = (List<SequenceInformation>) new ExampleDialect()
				.getSequenceInformationExtractor()
				.extractMetadata( context );
		final SequenceInformation result = results.get( 0 );
		assertEquals( "select * from fixture_sequences", query.get() );
		assertEquals( "fixture_sequence", result.getSequenceName().getSequenceName().getText() );
		assertNull( result.getSequenceName().getCatalogName() );
		assertEquals( "fixture_schema", result.getSequenceName().getSchemaName().getText() );
		assertNull( result.getStartValue() );
		assertEquals( 2L, result.getMinValue() );
		assertEquals( 200L, result.getMaxValue() );
		assertEquals( new BigDecimal( "2.5" ), result.getIncrementValue() );
	}

	private static ResultSet fixtureRows() throws SQLException {
		final var metadata = new RowSetMetaDataImpl();
		metadata.setColumnCount( 5 );
		setColumn( metadata, 1, "fixture_name", Types.VARCHAR );
		setColumn( metadata, 2, "fixture_schema", Types.VARCHAR );
		setColumn( metadata, 3, "fixture_minimum", Types.BIGINT );
		setColumn( metadata, 4, "fixture_maximum", Types.BIGINT );
		setColumn( metadata, 5, "fixture_increment", Types.DECIMAL );

		final var rows = RowSetProvider.newFactory().createCachedRowSet();
		rows.setMetaData( metadata );
		rows.moveToInsertRow();
		rows.updateString( "fixture_name", "fixture_sequence" );
		rows.updateString( "fixture_schema", "fixture_schema" );
		rows.updateLong( "fixture_minimum", 2L );
		rows.updateLong( "fixture_maximum", 200L );
		rows.updateBigDecimal( "fixture_increment", new BigDecimal( "2.5" ) );
		rows.insertRow();
		rows.moveToCurrentRow();
		rows.beforeFirst();
		return rows;
	}

	private static void setColumn(RowSetMetaDataImpl metadata, int position, String name, int type)
			throws SQLException {
		metadata.setColumnName( position, name );
		metadata.setColumnLabel( position, name );
		metadata.setColumnType( position, type );
	}
}
