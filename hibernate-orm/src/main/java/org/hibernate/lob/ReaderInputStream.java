/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.lob;

import java.io.IOException;
import java.io.Reader;

/**
 * Exposes a {@link java.io.Reader} as an {@link java.io.InputStream}.
 *
 * @deprecated Should not be used anymore. 
 */
@Deprecated
public class ReaderInputStream extends org.hibernate.engine.jdbc.ReaderInputStream {

	public ReaderInputStream(Reader reader) {
		super(reader);
	}

	public int read() throws IOException {
		return super.read();
	}

}
