/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;
import java.util.List;
import java.util.function.Function;

import org.hibernate.tool.schema.spi.ScriptSourceInput;

/**
 * Convenience base class for ScriptSourceInput implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractScriptSourceInput implements ScriptSourceInput {

	protected abstract Reader prepareReader();

	protected abstract void releaseReader(Reader reader);

	@Override
	public List<String> extract(Function<Reader, List<String>> extractor) {
		final Reader inputReader = prepareReader();

		try {
			return extractor.apply( inputReader );
		}
		finally {
			releaseReader( inputReader );
		}
	}
}
