/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.type.descriptor.java.DataHelper;

/**
 * Implementation of {@link CharacterStream}
 *
 * @author Steve Ebersole
 */
public class CharacterStreamImpl implements CharacterStream {
	private final long length;

	private Reader reader;
	private String string;

	/**
	 * Constructs a CharacterStreamImpl
	 *
	 * @param chars The String of characters to use backing the CharacterStream
	 */
	public CharacterStreamImpl(String chars) {
		this.string = chars;
		this.length = chars.length();
	}

	/**
	 * Constructs a CharacterStreamImpl
	 *
	 * @param reader The Reader containing the characters to use backing the CharacterStream
	 * @param length The length of the stream
	 */
	public CharacterStreamImpl(Reader reader, long length) {
		this.reader = reader;
		this.length = length;
	}

	@Override
	public Reader asReader() {
		if ( reader == null ) {
			reader = new StringReader( string );
		}
		return reader;
	}

	@Override
	public String asString() {
		if ( string == null ) {
			string = DataHelper.extractString( reader );
		}
		return string;
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public void release() {
		if ( reader == null ) {
			return;
		}
		try {
			reader.close();
		}
		catch (IOException ignore) {
		}
	}
}
