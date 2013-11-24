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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import javax.persistence.PersistenceException;

import org.jboss.logging.Logger;

/**
 * ScriptSourceInput implementation for File references.  A reader is opened here and then explicitly closed on
 * {@link #release}.
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromFile extends ScriptSourceInputFromReader implements ScriptSourceInput {
	private static final Logger log = Logger.getLogger( ScriptSourceInputFromFile.class );

	/**
	 * Constructs a ScriptSourceInputFromFile
	 *
	 * @param file The file to read from
	 */
	public ScriptSourceInputFromFile(File file) {
		super( toFileReader( file ) );
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
	private static Reader toFileReader(File file) {
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
			return new FileReader( file );
		}
		catch (IOException e) {
			throw new PersistenceException(
					"Unable to open specified script target file [" + file + "] for reading",
					e
			);
		}
	}

}
