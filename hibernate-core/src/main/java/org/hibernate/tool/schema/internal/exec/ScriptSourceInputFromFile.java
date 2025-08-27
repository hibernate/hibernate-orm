/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.jboss.logging.Logger;

/**
 * ScriptSourceInput implementation for File references.
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromFile extends AbstractScriptSourceInput {
	private static final Logger log = Logger.getLogger( ScriptSourceInputFromFile.class );

	private final File file;
	private final String charsetName;

	/**
	 * Constructs a ScriptSourceInputFromFile
	 *
	 * @param file The file to read from
	 * @param charsetName The charset name
	 */
	public ScriptSourceInputFromFile(File file, String charsetName) {
		this.file = file;
		this.charsetName = charsetName;
	}

	@Override
	public String getScriptDescription() {
		return file.getAbsolutePath();
	}

	@Override
	protected Reader prepareReader() {
		return toReader( file, charsetName );
	}

	private static Reader toReader(File file, String charsetName) {
		if ( ! file.exists() ) {
			log.warnf( "Specified schema generation script file [%s] did not exist for reading", file );
			return new Reader() {
				@Override
				public int read(char[] cbuf, int off, int len) throws IOException {
					return -1;
				}

				@Override
				public void close() throws IOException {
				}
			};
		}

		try {
			return charsetName != null ?
				new InputStreamReader( new FileInputStream(file), charsetName ) :
				new InputStreamReader( new FileInputStream(file) );
		}
		catch (IOException e) {
			throw new SchemaManagementException(
					"Unable to open specified script target file [" + file + "] for reading",
					e
			);
		}
	}

	@Override
	protected void releaseReader(Reader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			log.warn( "Unable to close file reader for generation script source" );
		}
	}

	@Override
	public boolean containsScript(URL url) {
		return file.getAbsolutePath().equals( url.getPath() );
	}

	@Override
	public String toString() {
		return "ScriptSourceInputFromFile(" + file.getAbsolutePath() + ")";
	}
}
