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

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * ScriptSourceInput implementation for explicitly given Readers.  The readers are not released by this class.
 *
 * @author Steve Ebersole
 */
public class ScriptSourceInputFromReader implements ScriptSourceInput {
	private final Reader reader;

	/**
	 * Constructs a ScriptSourceInputFromReader
	 *
	 * @param reader The reader to read from
	 */
	public ScriptSourceInputFromReader(Reader reader) {
		this.reader = reader;
	}

	@Override
	public Iterable<String> read(ImportSqlCommandExtractor commandExtractor) {
		final String[] commands = commandExtractor.extractCommands( reader );
		if ( commands == null ) {
			return Collections.emptyList();
		}
		else {
			return Arrays.asList( commands );
		}
	}

	@Override
	public void release() {
		// nothing to do here
	}

	protected Reader reader() {
		return reader;
	}
}
