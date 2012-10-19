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
package org.hibernate.engine.jdbc;

import java.io.InputStream;

/**
 * Wraps a binary stream to also provide the length which is needed when binding.
 *
 * @author Steve Ebersole
 */
public interface BinaryStream {
	/**
	 * Retrieve the input stream.
	 *
	 * @return The input stream
	 */
	public InputStream getInputStream();

	/**
	 * Access to the bytes.
	 *
	 * @return The bytes.
	 */
	public byte[] getBytes();

	/**
	 * Retrieve the length of the input stream
	 *
	 * @return The input stream length
	 */
	public long getLength();

	/**
	 * Release any underlying resources.
	 */
	public void release();
}
