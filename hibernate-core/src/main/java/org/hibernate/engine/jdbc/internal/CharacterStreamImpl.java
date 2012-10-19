/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.internal;

import java.io.IOException;
import java.io.InputStream;
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

	public CharacterStreamImpl(String chars) {
		this.string = chars;
		this.length = chars.length();
	}

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
