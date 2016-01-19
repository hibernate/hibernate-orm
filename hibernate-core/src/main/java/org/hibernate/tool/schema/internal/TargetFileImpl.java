/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class TargetFileImpl extends TargetBase {
	private final String outputFile;
	private final String delimiter;
	private final Formatter formatter;
	private FileWriter fileWriter;

	public TargetFileImpl(String outputFile, String delimiter) {
		this( outputFile, delimiter, FormatStyle.DDL.getFormatter() );
	}

	public TargetFileImpl(String outputFile, String delimiter, Formatter formatter) {
		this( Collections.<Exception>emptyList(), false, new SqlStatementLogger(), formatter, outputFile, delimiter );
	}

	public TargetFileImpl(List<Exception> exceptionCollector, boolean haltOnError, SqlStatementLogger sqlStatementLogger, Formatter formatter, String outputFile, String delimiter) {
		super( exceptionCollector, haltOnError, sqlStatementLogger, formatter );

			this.delimiter = delimiter;
			this.outputFile = outputFile;
			this.formatter = formatter;
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return false;
	}

	@Override
	public void prepare() {
		try {
			this.fileWriter = new FileWriter( outputFile, true );
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to open FileWriter [" + outputFile + "]", e );
		}
	}

	@Override
	public void doAccept(String action) {
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
