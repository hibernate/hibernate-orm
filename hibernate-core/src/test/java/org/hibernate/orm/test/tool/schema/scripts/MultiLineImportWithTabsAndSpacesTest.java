/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-14249")
public class MultiLineImportWithTabsAndSpacesTest {
	public static final String IMPORT_FILE = "org/hibernate/orm/test/tool/schema/scripts/multi-line-statements-starting-with-tabs-and-spaces.sql";

	private final MultiLineSqlScriptExtractor extractor = new MultiLineSqlScriptExtractor();

	@Test
	public void testExtraction() throws Exception {
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		try (final InputStream stream = classLoader.getResourceAsStream( IMPORT_FILE )) {
			assertThat( stream, notNullValue() );
			try (final InputStreamReader reader = new InputStreamReader( stream )) {
				final List<String> commands = extractor.extractCommands( reader, DialectContext.getDialect() );
				assertThat( commands, notNullValue() );
				assertThat( commands.size(), is( 3 ) );
			}
		}
	}
}
