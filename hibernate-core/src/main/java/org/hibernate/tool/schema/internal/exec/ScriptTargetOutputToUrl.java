/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.internal.CoreLogging;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

import org.jboss.logging.Logger;

/**
 * ScriptTargetOutput implementation for writing to supplied URL references
 *
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToUrl extends AbstractScriptTargetOutput implements ScriptTargetOutput {
	private static final Logger log = CoreLogging.logger( ScriptTargetOutputToUrl.class );

	private final URL url;
	private final String charsetName;
	private final boolean append;

	private Writer writer;

	/**
	 * Constructs a ScriptTargetOutputToUrl instance
	 *
	 * @param url The url to read from
	 * @param charsetName The charset name
	 * @param append If true, then bytes will be written to the end of the file rather than the beginning
	 */
	public ScriptTargetOutputToUrl(URL url, String charsetName, boolean append) {
		this.url = url;
		this.charsetName = charsetName;
		this.append = append;
	}

	/**
	 * Constructs a ScriptTargetOutputToUrl instance
	 * the bytes will be written to the end of the file rather than the beginning
	 *
	 * @param url The url to read from
	 * @param charsetName The charset name
	 */
	public ScriptTargetOutputToUrl(URL url, String charsetName) {
		this( url, charsetName, true );
	}

	@Override
	protected Writer writer() {
		if ( writer == null ) {
			throw new SchemaManagementException( "Illegal state : writer null - not prepared" );
		}
		return writer;
	}

	@Override
	public void prepare() {
		super.prepare();
		this.writer = toWriter( url, charsetName, append );
	}

	@Override
	public void release() {
		try {
			writer().close();
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to close file writer : " + e );
		}
	}


	private static Writer toWriter( URL url, String charsetName, boolean append ) {
		log.debug( "Attempting to resolve writer for URL : " + url );
		// technically only "strings corresponding to file URLs" are supported, which I take to mean URLs whose
		// protocol is "file"
		try {
			return ScriptTargetOutputToFile.toFileWriter( new File( url.toURI() ), charsetName, append );
		}
		catch (URISyntaxException e) {
			throw new SchemaManagementException(
					String.format(
							"Could not convert specified URL[%s] to a File reference",
							url
					),
					e
			);
		}
	}
}
