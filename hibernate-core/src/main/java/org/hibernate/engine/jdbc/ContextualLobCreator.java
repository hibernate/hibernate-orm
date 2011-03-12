/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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

import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Clob;
import java.sql.Connection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;

/**
 * {@link LobCreator} implementation using contextual creation against the JDBC {@link java.sql.Connection} class's LOB creation
 * methods.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ContextualLobCreator extends AbstractLobCreator implements LobCreator {
	private LobCreationContext lobCreationContext;

	public ContextualLobCreator(LobCreationContext lobCreationContext) {
		this.lobCreationContext = lobCreationContext;
	}

	/**
	 * Create the basic contextual BLOB reference.
	 *
	 * @return The created BLOB reference.
	 */
	public Blob createBlob() {
		return ( Blob ) lobCreationContext.execute( CREATE_BLOB_CALLBACK );
	}

	/**
	 * {@inheritDoc}
	 */
	public Blob createBlob(byte[] bytes) {
		try {
			Blob blob = createBlob();
			blob.setBytes( 1, bytes );
			return blob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set BLOB bytes after creation", e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Blob createBlob(InputStream inputStream, long length) {
		try {
			Blob blob = createBlob();
			OutputStream byteStream = blob.setBinaryStream( 1 );
			StreamUtils.copy( inputStream, byteStream );
			byteStream.flush();
			byteStream.close();
			// todo : validate length written versus length given?
			return blob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to prepare BLOB binary stream for writing",e );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to write stream contents to BLOB", e );
		}
	}

	/**
	 * Create the basic contextual CLOB reference.
	 *
	 * @return The created CLOB reference.
	 */
	public Clob createClob() {
		return ( Clob ) lobCreationContext.execute( CREATE_CLOB_CALLBACK );
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createClob(String string) {
		try {
			Clob clob = createClob();
			clob.setString( 1, string );
			return clob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set CLOB string after creation", e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createClob(Reader reader, long length) {
		try {
			Clob clob = createClob();
			Writer writer = clob.setCharacterStream( 1 );
			StreamUtils.copy( reader, writer );
			writer.flush();
			writer.close();
			return clob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to prepare CLOB stream for writing", e );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to write CLOB stream content", e );
		}
	}

	/**
	 * Create the basic contextual NCLOB reference.
	 *
	 * @return The created NCLOB reference.
	 */
	public Clob createNClob() {
		return ( Clob ) lobCreationContext.execute( CREATE_NCLOB_CALLBACK );
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createNClob(String string) {
		try {
			Clob clob = createNClob();
			clob.setString( 1, string );
			return clob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set NCLOB string after creation", e );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createNClob(Reader reader, long length) {
		try {
			Clob clob = createNClob();
			Writer writer = clob.setCharacterStream( 1 );
			StreamUtils.copy( reader, writer );
			writer.flush();
			writer.close();
			return clob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to prepare NCLOB stream for writing", e );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to write NCLOB stream content", e );
		}
	}


	private static final Class[] CREATION_METHOD_SIG = new Class[0];
	private static final Object[] CREATION_METHOD_ARGS = new Object[0];

	private static final LobCreationContext.Callback CREATE_BLOB_CALLBACK;
	private static final LobCreationContext.Callback CREATE_CLOB_CALLBACK;
	private static final LobCreationContext.Callback CREATE_NCLOB_CALLBACK;

	static {
		CREATE_BLOB_CALLBACK = new CallbackImpl( getConnectionlobCreationMethod( "createBlob" ) );
		CREATE_CLOB_CALLBACK = new CallbackImpl( getConnectionlobCreationMethod( "createClob" ) );
		CREATE_NCLOB_CALLBACK = new CallbackImpl( getConnectionlobCreationMethod( "createNClob" ) );
	}

	private static class CallbackImpl implements LobCreationContext.Callback {
		private final Method creationMethod;

		private CallbackImpl(Method creationMethod) {
			this.creationMethod = creationMethod;
		}

		public Object executeOnConnection(Connection connection) throws SQLException {
			try {
				return creationMethod.invoke( connection, CREATION_METHOD_ARGS );
			}
			catch ( InvocationTargetException e ) {
				if ( e.getTargetException() instanceof SQLException ) {
					throw ( SQLException ) e.getTargetException();
				}
				else {
					throw new HibernateException( "Exception invoking " + creationMethod.getName(), e.getTargetException() );
				}
			}
			catch ( AbstractMethodError e ) {
				// this again is a big big error...
				throw new IllegalStateException( "Useable implementation of " + creationMethod.getName() + " not found." );
			}
			catch ( IllegalAccessException e ) {
				// this again is a big big error...
				throw new IllegalStateException( "Illegal access attempt on JDBC method " + creationMethod.getName() );
			}
		}
	}

	private static Method getConnectionlobCreationMethod(String methodName) {
		try {
			return Connection.class.getMethod( methodName, CREATION_METHOD_SIG );
		}
		catch ( NoSuchMethodException e ) {
			// this is a big big error if we get here and these methods are not part of the Connection interface...
			throw new IllegalStateException( "JDBC driver did not implement " + methodName);
		}
	}
}
