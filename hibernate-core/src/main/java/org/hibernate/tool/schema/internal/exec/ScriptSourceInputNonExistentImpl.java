/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


/**
 * Used in cases where a specified source cannot be found
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputNonExistentImpl extends AbstractScriptSourceInput {
	/**
	 * Singleton access
	 */
	public static final ScriptSourceInputNonExistentImpl INSTANCE = new ScriptSourceInputNonExistentImpl();

	@Override
	public String getScriptDescription() {
		return "[injected ScriptSourceInputNonExistentImpl script]";
	}

	@Override
	protected Reader prepareReader() {
		return null;
	}

	@Override
	protected void releaseReader(Reader reader) {

	}

	@Override
	public List<String> extract(Function<Reader, List<String>> extractor) {
		return Collections.emptyList();
	}

	@Override
	public boolean exists() {
		return false;
	}
}
