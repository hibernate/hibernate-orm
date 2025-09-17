/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.jboss.logging.Logger;

/**
 * ScriptSourceInput implementation for URL references.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromUrl extends AbstractScriptSourceInput {
	private static final Logger LOG = Logger.getLogger( ScriptSourceInputFromFile.class );

	private final URL url;
	private final String charsetName;

	/**
	 * Constructs a ScriptSourceInputFromUrl instance
	 *
	 * @param url The url to read from
	 * @param charsetName The charset name
	 */
	public ScriptSourceInputFromUrl(URL url, String charsetName) {
		this.url = url;
		this.charsetName = charsetName;
	}

	@Override
	public String getScriptDescription() {
		return url.toExternalForm();
	}

	@Override
	protected Reader prepareReader() {
		try {
			final var stream = url.openStream();
			return charsetName != null
					? new InputStreamReader( stream, charsetName )
					: new InputStreamReader( stream );
		}
		catch (IOException e) {
			throw new SchemaManagementException(
					"Unable to open specified script source url [" + url + "] for reading (" + charsetName + ")"
			);
		}
	}

	@Override
	protected void releaseReader(Reader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			LOG.warn( "Unable to close file reader for generation script source" );
		}
	}

	@Override
	public boolean containsScript(URL url) {
		return this.url.equals( url );
	}

	@Override
	public String toString() {
		return "ScriptSourceInputFromUrl(" + url.toExternalForm() + ")";
	}
}
