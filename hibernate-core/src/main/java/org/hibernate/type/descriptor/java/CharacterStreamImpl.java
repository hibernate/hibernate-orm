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
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.StringReader;

import org.hibernate.type.descriptor.CharacterStream;

/**
 * Implementation of {@link CharacterStream}
 *
 * @author Steve Ebersole
 */
public class CharacterStreamImpl implements CharacterStream {
	private final StringReader reader;
	private final int length;

	public CharacterStreamImpl(String chars) {
		reader = new StringReader( chars );
		length = chars.length();
	}

	public Reader getReader() {
		return reader;
	}

	public int getLength() {
		return length;
	}
}
