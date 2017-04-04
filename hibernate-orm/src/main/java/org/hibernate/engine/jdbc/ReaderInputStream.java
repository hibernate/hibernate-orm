/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Exposes a {@link Reader} as an {@link InputStream}.
 * 
 * @author Gavin King
 */
public class ReaderInputStream extends InputStream {
	private Reader reader;

	/**
	 * Constructs a ReaderInputStream from a Reader
	 *
	 * @param reader The reader to expose as an InputStream
	 */
	public ReaderInputStream(Reader reader) {
		this.reader = reader;
	}

	@Override
	public int read() throws IOException {
		return reader.read();
	}
}
