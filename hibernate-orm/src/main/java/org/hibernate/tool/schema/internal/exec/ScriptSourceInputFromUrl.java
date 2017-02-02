/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptSourceInput;

import org.jboss.logging.Logger;

/**
 * ScriptSourceInput implementation for URL references.  A reader is opened here and then explicitly closed on
 * {@link #release}.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromUrl extends AbstractScriptSourceInput implements ScriptSourceInput {
	private static final Logger log = Logger.getLogger( ScriptSourceInputFromFile.class );

	private final URL url;
	private final String charsetName;

	private Reader reader;

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
	protected Reader reader() {
		if ( reader == null ) {
			throw new SchemaManagementException( "Illegal state - reader is null - not prepared" );
		}
		return reader;
	}

	@Override
	public void prepare() {
		super.prepare();
		try {
			this.reader = charsetName != null ?
				new InputStreamReader( url.openStream(), charsetName ) :
				new InputStreamReader( url.openStream() );
		}
		catch (IOException e) {
			throw new SchemaManagementException(
					"Unable to open specified script source url [" + url + "] for reading"
			);
		}
	}

	@Override
	public void release() {
		try {
			reader().close();
		}
		catch (IOException e) {
			log.warn( "Unable to close file reader for generation script source" );
		}
	}

	@Override
	public String toString() {
		return "ScriptSourceInputFromUrl(" + url.toExternalForm() + ")";
	}
}
