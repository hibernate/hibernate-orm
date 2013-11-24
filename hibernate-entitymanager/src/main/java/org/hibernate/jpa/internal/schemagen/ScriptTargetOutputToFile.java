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
