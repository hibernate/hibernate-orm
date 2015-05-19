/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * ScriptSourceInput implementation for explicitly given Readers.  The readers are not released by this class.
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromReader implements ScriptSourceInput {
	private final Reader reader;

	/**
	 * Constructs a ScriptSourceInputFromReader
	 *
	 * @param reader The reader to read from
	 */
	public ScriptSourceInputFromReader(Reader reader) {
		this.reader = reader;
	}

	@Override
	public Iterable<String> read(ImportSqlCommandExtractor commandExtractor) {
		final String[] commands = commandExtractor.extractCommands( reader );
		if ( commands == null ) {
			return Collections.emptyList();
		}
		else {
			return Arrays.asList( commands );
		}
	}

	@Override
	public void release() {
		// nothing to do here
	}

	protected Reader reader() {
		return reader;
	}
}
