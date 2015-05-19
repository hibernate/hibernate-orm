/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.internal.HEMLogging;

import org.jboss.logging.Logger;

/**
 * ScriptTargetOutput implementation for writing to supplied File references
 *
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToFile extends ScriptTargetOutputToWriter implements ScriptTargetOutput {
	private static final Logger log = HEMLogging.logger( ScriptTargetOutputToFile.class );

	/**
	 * Constructs a ScriptTargetOutputToFile
	 *
	 * @param file The file to write to
	 */
	public ScriptTargetOutputToFile(File file) {
		super( toFileWriter( file ) );
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

	@SuppressWarnings("ResultOfMethodCallIgnored")
	static Writer toFileWriter(File file) {
		try {
			if ( ! file.exists() ) {
				// best effort, since this is very likely not allowed in EE environments
				log.debug( "Attempting to create non-existent script target file : " + file.getAbsolutePath() );
				if ( file.getParentFile() != null ) {
					file.getParentFile().mkdirs();
				}
				file.createNewFile();
			}
		}
		catch (Exception e) {
			log.debug( "Exception calling File#createNewFile : " + e.toString() );
		}
		try {
			return new FileWriter( file );
		}
		catch (IOException e) {
			throw new PersistenceException( "Unable to open specified script target file for writing : " + file, e );
		}
	}
}
