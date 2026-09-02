/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.ColumnAliasExtractor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the stock result-column alias extraction strategies.
///
/// @author Steve Ebersole
public class ColumnAliasExtractorTest {
	@Test
	void stockExtractorsForwardTheJdbcPosition() throws SQLException {
		final ResultSetMetaData metaData = mock( ResultSetMetaData.class );
		when( metaData.getColumnLabel( 3 ) ).thenReturn( "projected_alias" );
		when( metaData.getColumnName( 4 ) ).thenReturn( "physical_name" );

		assertThat( ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR.extractColumnAlias( metaData, 3 ) )
				.isEqualTo( "projected_alias" );
		assertThat( ColumnAliasExtractor.COLUMN_NAME_EXTRACTOR.extractColumnAlias( metaData, 4 ) )
				.isEqualTo( "physical_name" );
		verify( metaData ).getColumnLabel( 3 );
		verify( metaData ).getColumnName( 4 );
	}

	@Test
	void checkedExceptionIsPropagated() throws SQLException {
		final ResultSetMetaData metaData = mock( ResultSetMetaData.class );
		final SQLException failure = new SQLException( "metadata unavailable" );
		when( metaData.getColumnLabel( 1 ) ).thenThrow( failure );

		assertThatThrownBy( () -> ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR.extractColumnAlias( metaData, 1 ) )
				.isSameAs( failure );
	}

	@Test
	void rootDefaultIsStable() {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {};
		assertThat( dialect.getColumnAliasExtractor() ).isSameAs( ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR );
	}
}
