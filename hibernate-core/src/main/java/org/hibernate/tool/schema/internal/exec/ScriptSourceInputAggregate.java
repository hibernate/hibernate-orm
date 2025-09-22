/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hibernate.tool.schema.spi.ScriptSourceInput;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * A script source input that aggregates over multiple other {@link ScriptSourceInput}.
 *
 * @author Christian Beikov
 */
public class ScriptSourceInputAggregate implements ScriptSourceInput {

	private final AbstractScriptSourceInput[] inputs;

	/**
	 * Constructs a ScriptSourceInputAggregate
	 *
	 * @param inputs The script source inputs
	 */
	public ScriptSourceInputAggregate(AbstractScriptSourceInput[] inputs) {
		this.inputs = inputs;
	}

	@Override
	public List<String> extract(Function<Reader, List<String>> extractor) {

		final List<String>[] lists = new List[inputs.length];
		int size = 0;
		for ( int i = 0; i < inputs.length; i++ ) {
			final var scriptSourceInput = inputs[i];
			if ( scriptSourceInput.exists() ) {
				final var reader = scriptSourceInput.prepareReader();
				try {
					CORE_LOGGER.executingScript( scriptSourceInput.getScriptDescription() );
					lists[i] = extractor.apply( reader );
					size += lists[i].size();
				}
				finally {
					scriptSourceInput.releaseReader( reader );
				}
			}
		}
		final List<String> list = new ArrayList<>( size );
		for ( var strings : lists ) {
			list.addAll( strings );
		}

		return list;
	}

	@Override
	public boolean containsScript(URL url) {
		for ( var input : inputs ) {
			if ( input.containsScript( url ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "ScriptSourceInputAggregate(" + Arrays.toString( inputs ) + ")";
	}
}
