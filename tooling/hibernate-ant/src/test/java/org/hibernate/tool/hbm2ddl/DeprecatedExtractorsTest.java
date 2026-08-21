/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor;
import org.hibernate.tool.schema.internal.script.SqlScriptExtractorInitiator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("deprecation")
public class DeprecatedExtractorsTest {

	@Test
	public void testSingleLineSqlCommandExtractor() {
		SingleLineSqlCommandExtractor extractor = new SingleLineSqlCommandExtractor();
		assertNotNull(extractor);
		assertInstanceOf(SingleLineSqlScriptExtractor.class, extractor);
	}

	@Test
	public void testMultipleLinesSqlCommandExtractor() {
		MultipleLinesSqlCommandExtractor extractor = new MultipleLinesSqlCommandExtractor();
		assertNotNull(extractor);
		assertInstanceOf(MultiLineSqlScriptExtractor.class, extractor);
	}

	@Test
	public void testImportSqlCommandExtractorInitiator() {
		ImportSqlCommandExtractorInitiator initiator = new ImportSqlCommandExtractorInitiator();
		assertNotNull(initiator);
		assertInstanceOf(SqlScriptExtractorInitiator.class, initiator);
	}
}
