/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.ResultSetMetaData;

import org.hibernate.dialect.jdbc.spi.ColumnAliasExtractor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies the standalone provider's column-name extraction choice.
///
/// @author Steve Ebersole
public class ExampleColumnAliasExtractorTest {
	@Test
	void suppliesTheStockColumnNameExtractor() {
		assertSame( ColumnAliasExtractor.COLUMN_NAME_EXTRACTOR, new ExampleDialect().getColumnAliasExtractor() );
	}

	@Test
	void stockChoiceCallsColumnName() throws Exception {
		final String[] calledMethod = new String[1];
		final int[] calledPosition = new int[1];
		final ResultSetMetaData metaData = (ResultSetMetaData) java.lang.reflect.Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { ResultSetMetaData.class },
				(proxy, method, arguments) -> {
					calledMethod[0] = method.getName();
					calledPosition[0] = (int) arguments[0];
					return "physical_name";
				}
		);

		assertEquals(
				"physical_name",
				new ExampleDialect().getColumnAliasExtractor().extractColumnAlias( metaData, 5 )
		);
		assertEquals( "getColumnName", calledMethod[0] );
		assertEquals( 5, calledPosition[0] );
	}
}
