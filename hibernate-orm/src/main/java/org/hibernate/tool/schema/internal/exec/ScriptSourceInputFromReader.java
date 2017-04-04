/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;

import org.hibernate.tool.schema.spi.ScriptSourceInput;

/**
 * ScriptSourceInput implementation for explicitly given Readers.  The readers are not released by this class.
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromReader extends AbstractScriptSourceInput implements ScriptSourceInput {
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
	protected Reader reader() {
		return reader;
	}

	@Override
	public String toString() {
		return "ScriptSourceInputFromReader()";
	}
}
