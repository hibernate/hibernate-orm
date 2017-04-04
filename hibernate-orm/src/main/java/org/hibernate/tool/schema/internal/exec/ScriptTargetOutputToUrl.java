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

	private Writer writer;

	/**
	 * Constructs a ScriptTargetOutputToUrl instance
	 *
	 * @param url The url to read from
	 * @param charsetName The charset name
	 */
	public ScriptTargetOutputToUrl(URL url, String charsetName) {
		this.url = url;
		this.charsetName = charsetName;
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
		this.writer = toWriter( url, charsetName );
	}

	@Override
	public void release() {
		try {
			writer().close();
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to close file writer : " + e.toString() );
		}
	}


	private static Writer toWriter( URL url, String charsetName ) {
		log.debug( "Attempting to resolve writer for URL : " + url );
		// technically only "strings corresponding to file URLs" are supported, which I take to mean URLs whose
		// protocol is "file"
		try {
			return ScriptTargetOutputToFile.toFileWriter( new File( url.toURI() ), charsetName );
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
