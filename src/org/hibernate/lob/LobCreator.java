//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.lob;

import java.sql.Clob;
import java.sql.Blob;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;

import org.hibernate.HibernateException;

/**
 * This interface defines the API for creating Blobs and Clobs.
 *
 * @author Gail Badner
 */
public interface LobCreator {

	/**
	 * Returns a Blob object representing a SQL BLOB created from the given array of bytes.
	 *
	 * @param bytes The array of bytes to be written to this BLOB object.
	 * @return the created Blob
	 * @throws HibernateException if the Blob could not be created
	 */
	Blob createBlob(byte[] bytes) throws HibernateException;

	/**
	 * Returns a Blob object representing a SQL BLOB created from the given number of bytes
	 * from an InputStream.
	 *
	 * @param is The input stream of bytes to be written to this BLOB object.
	 * @param length The number of bytes from stream to be written to this BLOB object.
	 * @return the created Blob
	 * @throws HibernateException if the Blob could not be created
	 * @throws IOException if an I/O error occurs
	 */
	Blob createBlob(InputStream is, int length) throws HibernateException, IOException;


	/**
	 * Returns a Blob object representing a SQL BLOB created from the available (is.available())
	 * number of bytes from an InputStream.
	 *
	 * @param is The input stream of bytes to be written to this BLOB object.
	 * @return the created Blob
	 * @throws HibernateException if the Blob could not be created
	 * @throws IOException if an I/O error occurs
	 */
	Blob createBlob(InputStream is) throws HibernateException, IOException;

	/**
	 * Returns a Clob object representing a SQL CLOB created from the given String.
	 *
	 * @param string The String to be written to this CLOB object
	 * @throws HibernateException if the Clob could not be created
	 * @throws HibernateException
	 */
	Clob createClob(String string) throws HibernateException;

	/**
	 * Returns a Clob object representing a SQL CLOB created from the given number of
	 * characters from a character stream.
	 * 
	 * @param reader The character stream to be written to this CLOB object.
	 * @param length The number of characters from reader to be written to this CLOB object.
	 * @return the created Clob
	 * @throws HibernateException if the Clob could not be created
	 * @throws IOException if an I/O error occurs
	 */
	Clob createClob(Reader reader, int length) throws HibernateException, IOException;
}