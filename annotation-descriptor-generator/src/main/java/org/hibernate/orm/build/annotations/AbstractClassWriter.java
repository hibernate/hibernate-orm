/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractClassWriter {
	protected final Writer writer;

	protected AbstractClassWriter(Writer writer) {
		this.writer = writer;
	}

	protected void writeLine() throws IOException {
		writer.write( '\n' );
	}

	protected void writeLine(String line, Object... args) throws IOException {
		writer.write( String.format( Locale.ROOT, line, args ) );
		writeLine();
	}

	protected void writeLine(int indentation, String line, Object... args) throws IOException {
		writer.write( " " .repeat( indentation * 4 ) );
		writeLine( line, args );
	}
}
