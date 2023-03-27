/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;
import java.net.URL;

/**
 * ScriptSourceInput implementation for explicitly given Readers.
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromReader extends AbstractScriptSourceInput {
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
	public String getScriptDescription() {
		return "[injected ScriptSourceInputFromReader script]";
	}

	@Override
	protected Reader prepareReader() {
		return reader;
	}

	@Override
	protected void releaseReader(Reader reader) {
		// nothing to do
	}

	@Override
	public boolean containsScript(URL url) {
		return false;
	}

	@Override
	public String toString() {
		return "ScriptSourceInputFromReader()";
	}
}
