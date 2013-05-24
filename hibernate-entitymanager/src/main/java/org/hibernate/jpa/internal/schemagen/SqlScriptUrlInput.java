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

import javax.persistence.PersistenceException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.logging.Logger;

/**
 * SqlScriptInput implementation for URL references.  A reader is opened here and then explicitly closed on
 * {@link #release}.
 *
 * @author Christian Beikov
 */
class SqlScriptUrlInput extends SqlScriptReaderInput implements SqlScriptInput {
	private static final Logger log = Logger.getLogger( SqlScriptUrlInput.class );

	public SqlScriptUrlInput(String fileUrl) {
		super( toReader( fileUrl ) );
	}
	
	public SqlScriptUrlInput(URL fileUrl) {
		super( toReader( fileUrl ) );
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

	private static Reader toReader(String fileUrl) {
		try {
			return toReader( new URL( fileUrl ) );
		}
		catch ( MalformedURLException e ) {
			throw new PersistenceException(
					"Invalid url specified for script target file [" + fileUrl + "] for reading",
					e
			);
		}
	}
	
	private static Reader toReader(URL fileUrl) {
		try {
			return new InputStreamReader(fileUrl.openStream());
		}
		catch ( IOException e ) {
			throw new PersistenceException(
					"Unable to open specified script target file [" + fileUrl + "] for reading",
					e
			);
		}
	}

}
