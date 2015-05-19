/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.schemagen;

import java.io.IOException;
import java.io.Writer;
import javax.persistence.PersistenceException;

import org.hibernate.internal.util.StringHelper;

/**
 * ScriptTargetOutput implementation for supplied Writer references
 *
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToWriter implements ScriptTargetOutput {
	private static final String NEWLINE;
	static {
		final String systemNewLine = System.getProperty( "line.separator" );
		NEWLINE = StringHelper.isNotEmpty( systemNewLine ) ? systemNewLine : "\n";
	}

	private final Writer writer;

	/**
	 * Constructs a ScriptTargetOutputToWriter
	 *
	 * @param writer The writer to write to
	 */
	public ScriptTargetOutputToWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void accept(String command) {
		try {
			writer.write( command );
			writer.write( NEWLINE );
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
