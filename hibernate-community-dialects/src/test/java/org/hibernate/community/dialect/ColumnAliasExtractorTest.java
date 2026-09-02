/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies that community Dialects retain the inherited label extractor.
///
/// @author Steve Ebersole
public class ColumnAliasExtractorTest {
	@Test
	void communityDialectUsesTheInheritedLabelChoice() throws SQLException {
		final ResultSetMetaData metaData = mock( ResultSetMetaData.class );
		when( metaData.getColumnLabel( 2 ) ).thenReturn( "community_alias" );

		assertThat( new H2LegacyDialect().getColumnAliasExtractor().extractColumnAlias( metaData, 2 ) )
				.isEqualTo( "community_alias" );
		verify( metaData ).getColumnLabel( 2 );
	}
}
