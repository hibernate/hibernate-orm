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
import java.io.Reader;

import org.jboss.logging.Logger;

/**
 * SqlScriptInput implementation for File references.  A reader is opened here and then explicitly closed on
 * {@link #release}.
 *
 * @author Steve Ebersole
 */
class SqlScriptFileInput extends SqlScriptReaderInput implements SqlScriptInput {
	private static final Logger log = Logger.getLogger( SqlScriptFileInput.class );

	public SqlScriptFileInput(String fileUrl) {
		super( toFileReader( fileUrl ) );
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

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static Reader toFileReader(String fileUrl) {
		final File file = new File( fileUrl );
		if ( ! file.exists() ) {
			log.warnf( "Specified schema generation script file [%s] did not exist for reading", fileUrl );
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
			return new FileReader( file );
		}
		catch (IOException e) {
			throw new PersistenceException(
					"Unable to open specified script target file [" + fileUrl + "] for reading",
					e
			);
		}
	}

}
