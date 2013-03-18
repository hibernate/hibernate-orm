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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.jboss.logging.Logger;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.SchemaGenAction;

/**
 * GenerationTarget implementation for handling generation to scripts
 *
 * @author Steve Ebersole
 */
class ScriptsTarget implements GenerationTarget {
	private static final Logger log = Logger.getLogger( ScriptsTarget.class );

	private final ScriptTargetTarget createScriptTarget;
	private final ScriptTargetTarget dropScriptTarget;
	private final SchemaGenAction scriptsAction;

	public ScriptsTarget(
			Object createScriptTargetSetting,
			Object dropScriptTargetSetting,
			SchemaGenAction scriptsAction) {
		this.scriptsAction = scriptsAction;

		if ( scriptsAction.includesCreate() ) {
			if ( Writer.class.isInstance( createScriptTargetSetting ) ) {
				createScriptTarget = new WriterScriptTarget( (Writer) createScriptTargetSetting );
			}
			else {
				createScriptTarget = new FileScriptTarget( createScriptTargetSetting.toString() );
			}
		}
		else {
			if ( createScriptTargetSetting != null ) {
				// the wording in the spec hints that this maybe should be an error, but does not explicitly
				// call out an exception to use.
				log.debugf(
						"Value was specified for '%s' [%s], but create scripting was not requested",
						AvailableSettings.SCHEMA_GEN_SCRIPTS_CREATE_TARGET,
						createScriptTargetSetting
				);
			}
			createScriptTarget = null;
		}

		if ( scriptsAction.includesDrop() ) {
			if ( Writer.class.isInstance( dropScriptTargetSetting ) ) {
				dropScriptTarget = new WriterScriptTarget( (Writer) dropScriptTargetSetting );
			}
			else {
				dropScriptTarget = new FileScriptTarget( dropScriptTargetSetting.toString() );
			}
		}
		else {
			if ( dropScriptTargetSetting != null ) {
				// the wording in the spec hints that this maybe should be an error, but does not explicitly
				// call out an exception to use.
				log.debugf(
						"Value was specified for '%s' [%s], but drop scripting was not requested",
						AvailableSettings.SCHEMA_GEN_SCRIPTS_DROP_TARGET,
						dropScriptTargetSetting
				);
			}
			dropScriptTarget = null;
		}
	}

	@Override
	public void acceptCreateCommands(Iterable<String> commands) {
		if ( ! scriptsAction.includesCreate() ) {
			return;
		}

		for ( String command : commands ) {
			createScriptTarget.accept( command );
		}
	}

	@Override
	public void acceptDropCommands(Iterable<String> commands) {
		if ( ! scriptsAction.includesDrop() ) {
			return;
		}

		for ( String command : commands ) {
			dropScriptTarget.accept( command );
		}
	}

	@Override
	public void release() {
		createScriptTarget.release();
		dropScriptTarget.release();
	}

	/**
	 * Internal contract for handling Writer/File differences
	 */
	private static interface ScriptTargetTarget {
		public void accept(String command);
		public void release();
	}

	private static class WriterScriptTarget implements ScriptTargetTarget {
		private final Writer writer;

		public WriterScriptTarget(Writer writer) {
			this.writer = writer;
		}

		@Override
		public void accept(String command) {
			try {
				writer.write( command );
				writer.flush();
			}
			catch (IOException e) {
				throw new PersistenceException( "Could not write to target script file", e );
			}
		}

		@Override
		public void release() {
			// nothing to do for a supplied writer
		}

		protected Writer writer() {
			return writer;
		}
	}

	private static class FileScriptTarget extends WriterScriptTarget implements ScriptTargetTarget {
		public FileScriptTarget(String fileUrl) {
			super( toFileWriter( fileUrl ) );
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
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static Writer toFileWriter(String fileUrl) {
		final File file = new File( fileUrl );
		try {
			// best effort, since this is very well not allowed in EE environments
			file.createNewFile();
		}
		catch (Exception e) {
			log.debug( "Exception calling File#createNewFile : " + e.toString() );
		}
		try {
			return new FileWriter( file );
		}
		catch (IOException e) {
			throw new PersistenceException( "Unable to open specified script target file for writing : " + fileUrl );
		}
	}
}
