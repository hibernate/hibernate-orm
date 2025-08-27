/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
