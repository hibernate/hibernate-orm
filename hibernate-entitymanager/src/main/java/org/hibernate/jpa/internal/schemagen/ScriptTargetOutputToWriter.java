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
