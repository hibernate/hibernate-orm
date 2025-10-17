/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.Test;

public class SingleLineImportExtractorTest {
	public static final String IMPORT_FILE_COMMENTS_ONLY = "org/hibernate/orm/test/tool/schema/scripts/comments-only.sql";

	private final SingleLineSqlScriptExtractor extractor = new SingleLineSqlScriptExtractor();

	@Test
	public void testExtractionFromEmptyScript() {
		StringReader reader = new StringReader( "" );
		final List<String> commands = extractor.extractCommands( reader, DialectContext.getDialect() );
		assertThat( commands, notNullValue() );
		assertThat( commands.size(), is( 0 ) );
	}

	@Test
	@JiraKey(value = "HHH-16279")
	// Note this worked from the start as HHH-16279 only affects the multi-line extractor
	public void testExtractionFromCommentsOnlyScript() throws IOException {
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		try ( final InputStream stream = classLoader.getResourceAsStream( IMPORT_FILE_COMMENTS_ONLY ) ) {
			assertThat( stream, notNullValue() );
			try ( final InputStreamReader reader = new InputStreamReader( stream ) ) {
				final List<String> commands = extractor.extractCommands( reader, DialectContext.getDialect() );
				assertThat( commands, notNullValue() );
				assertThat( commands.size(), is( 0 ) );
			}
		}
	}
}
