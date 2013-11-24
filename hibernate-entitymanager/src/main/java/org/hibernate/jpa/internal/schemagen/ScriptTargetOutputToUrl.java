/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
