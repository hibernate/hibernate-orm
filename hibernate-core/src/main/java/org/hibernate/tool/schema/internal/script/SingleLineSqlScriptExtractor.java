/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;
import org.hibernate.tool.schema.spi.SqlScriptException;

/**
 * Class responsible for extracting SQL statements from import script. Treats each line as a complete SQL statement.
 * Comment lines shall start with {@code --}, {@code //} or {@code /*} character sequence.
 *
 * @author Lukasz Antoniak
 * @author Steve Ebersole
 */
public class SingleLineSqlScriptExtractor implements SqlScriptCommandExtractor {
	public static final String SHORT_NAME = "single-line";

	public static final SqlScriptCommandExtractor INSTANCE = new SingleLineSqlScriptExtractor();

	@Override
	public List<String> extractCommands(Reader reader, Dialect dialect) {
		final List<String> statementList = new LinkedList<>();

		final BufferedReader bufferedReader = new BufferedReader( reader );
		try {
			for ( String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine() ) {
				final String trimmedLine = line.trim();

				if ( trimmedLine.isEmpty() || isComment( trimmedLine ) ) {
					continue;
				}

				final String command;
				if ( trimmedLine.endsWith( ";" ) ) {
					command = trimmedLine.substring( 0, trimmedLine.length() - 1 );
				}
				else {
					command = trimmedLine;
				}

				statementList.add( command );
			}

			return statementList;
		}
		catch (IOException e) {
			throw new SqlScriptException( "Error during sql-script parsing.", e );
		}
	}

	private boolean isComment(final String line) {
		return line.startsWith( "--" ) || line.startsWith( "//" ) || line.startsWith( "/*" );
	}
}
