/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.internal.HEMLogging;

import org.jboss.logging.Logger;

/**
 * ScriptTargetOutput implementation for writing to supplied URL references
 *
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToUrl extends ScriptTargetOutputToWriter implements ScriptTargetOutput {
	private static final Logger log = HEMLogging.logger( ScriptTargetOutputToUrl.class );

	/**
	 * Constructs a ScriptTargetOutputToUrl
	 *
	 * @param url The url to write to
	 */
	public ScriptTargetOutputToUrl(URL url) {
		super( toWriter( url ) );
	}

	@Override
	public void release() {
		try {
			writer().close();
		}
		catch (IOException e) {
			throw new PersistenceException( "Unable to close file writer : " + e.toString() );
		}
	}


	private static Writer toWriter(URL url) {
		log.debug( "Attempting to resolve writer for URL : " + url );
		// technically only "strings corresponding to file URLs" are supported, which I take to mean URLs whose
		// protocol is "file"
		try {
			return ScriptTargetOutputToFile.toFileWriter( new File( url.toURI() ) );
		}
		catch (URISyntaxException e) {
			throw new PersistenceException(
					String.format(
							"Could not convert specified URL[%s] to a File reference",
							url
					),
					e
			);
		}
	}
}
