/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.io.Reader;

/**
 * Wraps a character stream (reader) to also provide the length (number of characters) which is needed
 * when binding.
 *
 * @author Steve Ebersole
 */
public interface CharacterStream {
	/**
	 * Provides access to the underlying data as a Reader.
	 *
	 * @return The reader.
	 */
	public Reader asReader();

	/**
	 * Provides access to the underlying data as a String.
	 *
	 * @return The underlying String data
	 */
	public String asString();

	/**
	 * Retrieve the number of characters.
	 *
	 * @return The number of characters.
	 */
	public long getLength();

	/**
	 * Release any underlying resources.
	 */
	public void release();
}
