// $Id: $
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
 */
package org.hibernate.lob;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.Reader;
import java.io.OutputStream;
import java.io.Writer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.engine.SessionImplementor;

/**
 * A LobCreator implementation that can be used with JVMs and JDBC drivers that support JDBC4. 
 *
 * @author Gail Badner
 */
public class LobCreatorImplJDBC4 implements LobCreator {
	private final SessionImplementor session;

	/* package */
	LobCreatorImplJDBC4(Session session) {
		this.session = ( SessionImplementor ) session;
	}

	/**
 	 * {@inheritDoc}
	 */
	public Blob createBlob(byte[] bytes) throws HibernateException {
		Blob blob = createBlob();
		try {
			blob.setBytes( 1, bytes );
		}
		catch ( SQLException e ) {
			throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							e,
							"Exception invoking adding data to a Blob."
						);
		}
		return blob;
	}

	/**
 	 * {@inheritDoc}
	 */
	public Blob createBlob(InputStream is, int length) throws HibernateException, IOException {
		Blob blob = createBlob();
		try {
			OutputStream os = blob.setBinaryStream(1);
			byte[] data = new byte[1];
			for ( int i = 0; i < length && is.read( data ) != -1; i++ ) {
				os.write( data );
			}
			os.flush();
			os.close();
			return blob;
		}
		catch ( SQLException e ) {
			throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							e,
							"Exception getting OutputStream to add data to a Blob."
						);
		}
	}

	/**
 	 * {@inheritDoc}
	 */
	public Blob createBlob(InputStream is) throws HibernateException, IOException {
		return createBlob( is, is.available() );
	}

	/**
 	 * {@inheritDoc}
	 */
	public Clob createClob(String string)  throws HibernateException {
		Clob clob = createClob();
		try {
			clob.setString( 1, string );
			return clob;
		}
		catch ( SQLException e ) {
			throw JDBCExceptionHelper.convert(
				session.getFactory().getSQLExceptionConverter(),
				e,
				"Exception adding a String to a Clob."
			);
		}
	}

	/**
 	 * {@inheritDoc}
	 */
	public Clob createClob(Reader reader, int length) throws HibernateException, IOException {
		Clob clob = createClob();
		try {
			Writer writer = clob.setCharacterStream( 1 );
			char[] data = new char[1];
			for ( int i = 0; i < length && reader.read( data ) != -1; i++ ) {
				writer.write( data );
			}
			writer.flush();
			writer.close();
			return clob;
		}
		catch ( SQLException e ) {
			throw JDBCExceptionHelper.convert(
				session.getFactory().getSQLExceptionConverter(),
				e,
				"Exception getting OutputStream to add data to a Clob."
			);
		}
	}

	private Blob createBlob() {
		final String CREATE_BLOB_METHOD_NAME = "createBlob";
		return ( Blob ) invokeConnectionMethod( CREATE_BLOB_METHOD_NAME );
	}

	private Clob createClob() {
		final String CREATE_CLOB_METHOD_NAME = "createClob";
		return ( Clob ) invokeConnectionMethod( CREATE_CLOB_METHOD_NAME );
	}

	private Object invokeConnectionMethod(String methodName) throws HibernateException {
		Connection connection = session.getJDBCContext().getConnectionManager().getConnection();
		final Object emptyArray[] = { };
		try {
			Method method = Connection.class.getMethod( methodName, new Class[0] );
			Object object = method.invoke( connection, emptyArray );
			session.getJDBCContext().getConnectionManager().afterStatement();
			return object;
		}
		catch ( NoSuchMethodException e ) {
			throw new HibernateException( getMethodString( connection, methodName ) + " not supported. Set " + Environment.USE_CONNECTION_FOR_LOB_CREATION + " to false.", e );
		}
		catch ( AbstractMethodError e ) {
			throw new HibernateException( "Implementation of " + getMethodString( connection, methodName ) + " not found. Set " + Environment.USE_CONNECTION_FOR_LOB_CREATION + " to false.", e );
		}
		catch ( InvocationTargetException e ) {
			if ( e.getTargetException() instanceof SQLException ) {
				throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							( SQLException ) e.getTargetException(),
							"Exception invoking " + getMethodString( connection, methodName )
						);
			}
			throw new HibernateException( "Exception invoking " + getMethodString( connection, methodName ), e.getTargetException() );
		}
		catch ( IllegalAccessException e ) {
			throw new HibernateException( "Cannot access " + getMethodString( connection, methodName ), e );
		}
	}

	private String getMethodString(Connection connection, String methodName) {
		return new StringBuffer()
				.append( connection.getClass().getName() )
				.append('.')
				.append( methodName )
				.append( "()" ).toString();
	}
}
