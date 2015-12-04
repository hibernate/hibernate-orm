/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.io.FileWriter;
import java.io.IOException;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.Target;

/**
 * @author Steve Ebersole
 */
public class TargetFileImpl implements Target {
	final private FileWriter fileWriter;
	final private String delimiter;
	final private Formatter formatter;

	public TargetFileImpl(String outputFile, String delimiter) {
		this( outputFile, delimiter, FormatStyle.NONE.getFormatter());
	}
	
	public TargetFileImpl(String outputFile, String delimiter, Formatter formatter) {
		try {
			this.delimiter = delimiter;
			this.fileWriter = new FileWriter( outputFile );
			this.formatter = formatter;
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to open FileWriter [" + outputFile + "]", e );
		}
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return true;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String action) {
		try {
			if (formatter != null) {
				action = formatter.format(action);
			}
			fileWriter.write( action );
			if ( delimiter != null ) {
				fileWriter.write( delimiter );
			}
			fileWriter.write( "\n" );
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to write to FileWriter", e );
		}
	}

	@Override
	public void release() {
		if ( fileWriter != null ) {
			try {
				fileWriter.close();
			}
			catch (IOException ignore) {
			}
		}
	}

}
