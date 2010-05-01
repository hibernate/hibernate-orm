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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DataHelper {
	private static final Logger log = LoggerFactory.getLogger( DataHelper.class );

	private static Class nClobClass;
	static {
		try {
			// NClobs are only JDBC 4 (JDK 1.6) and higher
			nClobClass = ReflectHelper.classForName( "java.sql.NClob", DataHelper.class );
		}
		catch ( ClassNotFoundException e ) {
			e.printStackTrace();
		}
	}

	public static boolean isNClob(Class type) {
		return nClobClass != null && nClobClass.isAssignableFrom( type );
	}

	public static String extractString(Reader reader) {
		// read the Reader contents into a buffer and return the complete string
		final StringBuilder stringBuilder = new StringBuilder();
		try {
			char[] buffer = new char[2048];
			while (true) {
				int amountRead = reader.read( buffer, 0, buffer.length );
				if ( amountRead == -1 ) {
					break;
				}
				stringBuilder.append( buffer, 0, amountRead );
			}
		}
		catch ( IOException ioe) {
			throw new HibernateException( "IOException occurred reading text", ioe );
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				log.warn( "IOException occurred closing stream", e );
			}
		}
		return stringBuilder.toString();
	}

	public static byte[] extractBytes(InputStream inputStream) {
		// read the stream contents into a buffer and return the complete byte[]
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);
		try {
			byte[] buffer = new byte[2048];
			while (true) {
				int amountRead = inputStream.read( buffer );
				if ( amountRead == -1 ) {
					break;
				}
				outputStream.write( buffer, 0, amountRead );
			}
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "IOException occurred reading a binary value", ioe );
		}
		finally {
			try {
				inputStream.close();
			}
			catch ( IOException e ) {
				log.warn( "IOException occurred closing input stream", e );
			}
			try {
				outputStream.close();
			}
			catch ( IOException e ) {
				log.warn( "IOException occurred closing output stream", e );
			}
		}
		return outputStream.toByteArray();
	}
}
