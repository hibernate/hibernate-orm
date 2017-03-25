/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.hibernate.internal.CoreLogging;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

import org.jboss.logging.Logger;

/**
 * ScriptTargetOutput implementation for writing to supplied File references
 *
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToFile extends AbstractScriptTargetOutput implements ScriptTargetOutput {
	private static final Logger log = CoreLogging.logger( ScriptTargetOutputToFile.class );

	private final File file;
	private final String charsetName;

	private Writer writer;

	/**
	 * Constructs a ScriptTargetOutputToFile instance
	 *
	 * @param file The file to read from
	 * @param charsetName The charset name
	 */
	public ScriptTargetOutputToFile(File file, String charsetName) {
		this.file = file;
		this.charsetName = charsetName;
	}

	@Override
	protected Writer writer() {
		if ( writer == null ) {
			throw new SchemaManagementException( "Illegal state : writer null - not prepared" );
		}
		return writer;
	}

	@Override
	public void prepare() {
		super.prepare();
		this.writer = toFileWriter( this.file, this.charsetName );
	}

	@Override
	public void release() {
		if ( writer != null ) {
			try {
				writer.close();
			}
			catch (IOException e) {
				throw new SchemaManagementException( "Unable to close file writer : " + e.toString() );
			}
			finally {
				writer = null;
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	static Writer toFileWriter( File file, String charsetName ) {
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
			return charsetName != null ?
					new OutputStreamWriter(
							new FileOutputStream( file, true ),
							charsetName
					) :
					new OutputStreamWriter( new FileOutputStream(
							file,
							true
					) );
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to open specified script target file for writing : " + file, e );
		}
	}
}
