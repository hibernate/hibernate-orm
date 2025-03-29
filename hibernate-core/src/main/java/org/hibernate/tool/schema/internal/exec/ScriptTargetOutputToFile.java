/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	private final boolean append;

	private Writer writer;

	/**
	 * Constructs a ScriptTargetOutputToFile instance
	 *
	 * @param file The file to read from
	 * @param charsetName The charset name
	 * @param append If true, then bytes will be written to the end of the file rather than the beginning
	 */
	public ScriptTargetOutputToFile(File file, String charsetName, boolean append) {
		this.file = file;
		this.charsetName = charsetName;
		this.append = append;
	}

	/**
	 * Constructs a ScriptTargetOutputToFile instance,
	 * the bytes will be written to the end of the file rather than the beginning
	 *
	 * @param file The file to read from
	 * @param charsetName The charset name
	 *
	 */
	public ScriptTargetOutputToFile(File file, String charsetName ) {
		this(file, charsetName, true);
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
		this.writer = toFileWriter( this.file, this.charsetName, append );
	}

	@Override
	public void release() {
		if ( writer != null ) {
			try {
				writer.close();
			}
			catch (IOException e) {
				throw new SchemaManagementException( "Unable to close file writer : " + e );
			}
			finally {
				writer = null;
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	static Writer toFileWriter(File file, String charsetName, boolean append) {
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
			log.debug( "Exception calling File#createNewFile : " + e );
		}
		try {
			return charsetName != null ?
					new OutputStreamWriter(
							new FileOutputStream( file, append ),
							charsetName
					) :
					new OutputStreamWriter( new FileOutputStream(
							file,
							append
					) );
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to open specified script target file for writing : " + file, e );
		}
	}
}
