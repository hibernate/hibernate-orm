/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
