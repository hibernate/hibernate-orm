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

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.spi.ScriptSourceInput;

/**
 * A script source input that aggregates over multiple other {@link ScriptSourceInput}.
 *
 * @author Christian Beikov
 */
public class ScriptSourceInputAggregate implements ScriptSourceInput {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SchemaCreatorImpl.class );

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
			final AbstractScriptSourceInput scriptSourceInput = inputs[i];
			if ( scriptSourceInput.exists() ) {
				final Reader reader = scriptSourceInput.prepareReader();
				try {
					LOG.executingScript( scriptSourceInput.getScriptDescription() );
					lists[i] = extractor.apply( reader );
					size += lists[i].size();
				}
				finally {
					scriptSourceInput.releaseReader( reader );
				}
			}
		}
		final List<String> list = new ArrayList<>( size );
		for ( List<String> strings : lists ) {
			list.addAll( strings );
		}

		return list;
	}

	@Override
	public boolean containsScript(URL url) {
		for ( int i = 0; i < inputs.length; i++ ) {
			if ( inputs[i].containsScript( url ) ) {
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
