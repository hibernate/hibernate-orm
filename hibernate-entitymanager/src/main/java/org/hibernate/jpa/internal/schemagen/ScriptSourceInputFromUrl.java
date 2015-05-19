/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import javax.persistence.PersistenceException;

import org.jboss.logging.Logger;

/**
 * ScriptSourceInput implementation for URL references.  A reader is opened here and then explicitly closed on
 * {@link #release}.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromUrl extends ScriptSourceInputFromReader implements ScriptSourceInput {
	private static final Logger log = Logger.getLogger( ScriptSourceInputFromFile.class );

	/**
	 * Constructs a ScriptSourceInputFromUrl instance
	 *
	 * @param url The url to read from
	 */
	public ScriptSourceInputFromUrl(URL url) {
		super( toReader( url ) );
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

	private static Reader toReader(URL url) {
		try {
			return new InputStreamReader( url.openStream() );

		}
		catch (IOException e) {
			throw new PersistenceException(
					"Unable to open specified script source url [" + url + "] for reading"
			);
		}
	}
}
