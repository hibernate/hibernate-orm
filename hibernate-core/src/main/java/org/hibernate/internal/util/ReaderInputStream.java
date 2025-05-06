/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Exposes a {@link Reader} as an {@link InputStream}.
 *
 * @author Gavin King
 */
public class ReaderInputStream extends InputStream {
	private final Reader reader;

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
