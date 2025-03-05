/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.proxy.NClobProxy;

/**
 * {@link LobCreator} which can use {@link Connection#createBlob} and {@link Connection#createClob},
 * but {@link java.sql.NClob} references are created locally.
 *
 * @author Steve Ebersole
 *
 * @see NClobProxy
 */
public class BlobAndClobCreator extends AbstractLobCreator implements LobCreator {

	/**
	 * Callback for performing contextual BLOB creation
	 */
	public static final LobCreationContext.Callback<Blob> CREATE_BLOB_CALLBACK = Connection::createBlob;

	/**
	 * Callback for performing contextual CLOB creation
	 */
	public static final LobCreationContext.Callback<Clob> CREATE_CLOB_CALLBACK = Connection::createClob;

	/**
	 * Callback for performing contextual NCLOB creation
	 */
	public static final LobCreationContext.Callback<NClob> CREATE_NCLOB_CALLBACK = Connection::createNClob;

	protected final LobCreationContext lobCreationContext;
	protected final boolean useConnectionToCreateLob;

	BlobAndClobCreator(LobCreationContext lobCreationContext, boolean useConnectionToCreateLob) {
		this.lobCreationContext = lobCreationContext;
		this.useConnectionToCreateLob = useConnectionToCreateLob;
	}

	/**
	 * Create the basic contextual BLOB reference.
	 *
	 * @return The created BLOB reference.
	 */
	Blob createBlob() {
		return lobCreationContext.fromContext( CREATE_BLOB_CALLBACK );
	}

	/**
	 * Create a {@link Blob} object after reading a {@code byte[]}
	 * array from a JDBC {@link ResultSet}.
	 */
	@Override
	public Blob createBlob(byte[] bytes) {
		final Blob blob = createBlob();
		try {
			blob.setBytes( 1, bytes );
			return blob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set BLOB bytes after creation", e );
		}
	}

	/**
	 * Create a {@link Blob} object after reading an {@link InputStream}
	 * from a JDBC {@link ResultSet}.
	 *
	 * @implNote
	 * It's very inefficient to use JDBC LOB locator creation to create
	 * a LOB with the contents of the given stream, since that requires
	 * reading the whole stream. So instead just wrap the given stream,
	 * just like what {@link NonContextualLobCreator} does.
	 */
	@Override
	public Blob createBlob(InputStream stream, long length) {
		return NonContextualLobCreator.INSTANCE.createBlob( stream, length );
	}

	/**
	 * Create the basic contextual CLOB reference.
	 *
	 * @return The created CLOB reference.
	 */
	Clob createClob() {
		return lobCreationContext.fromContext( CREATE_CLOB_CALLBACK );
	}

	/**
	 * Create the basic contextual NCLOB reference.
	 *
	 * @return The created NCLOB reference.
	 */
	NClob createNClob() {
		return lobCreationContext.fromContext( CREATE_NCLOB_CALLBACK );
	}

	/**
	 * Create a {@link Clob} object after reading a {@code String}
	 * from a JDBC {@link ResultSet}.
	 */
	@Override
	public Clob createClob(String string) {
		try {
			final Clob clob = createClob();
			clob.setString( 1, string );
			return clob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set CLOB string after creation", e );
		}
	}

	/**
	 * Create a {@link Clob} object after reading an {@link InputStream}
	 * from a JDBC {@link ResultSet}.
	 *
	 * @implNote
	 * It's very inefficient to use JDBC LOB locator creation to create
	 * a LOB with the contents of the given stream, since that requires
	 * reading the whole stream. So instead just wrap the given stream,
	 * just like what {@link NonContextualLobCreator} does.
	 */
	@Override
	public Clob createClob(Reader reader, long length) {
		return NonContextualLobCreator.INSTANCE.createClob( reader, length );
	}

	@Override
	public NClob createNClob(String string) {
		return NonContextualLobCreator.INSTANCE.createNClob( string );
	}

	@Override
	public NClob createNClob(Reader reader, long length) {
		return NonContextualLobCreator.INSTANCE.createNClob( reader, length );
	}

	/**
	 * Obtain a {@link Blob} instance which can be written to a JDBC
	 * {@link java.sql.PreparedStatement} using
	 * {@link java.sql.PreparedStatement#setBlob(int, Blob)}.
	 */
	@Override
	public Blob toJdbcBlob(Blob blob) {
		try {
			if ( useConnectionToCreateLob ) {
//				final Blob jdbcBlob = createBlob();
//				blob.getBinaryStream().transferTo( jdbcBlob.setBinaryStream(1) );
//				return jdbcBlob;
				return createBlob( blob.getBytes( 1, (int) blob.length() ) );
			}
			else {
				return super.toJdbcBlob( blob );
			}
		}
		catch (SQLException e) {
			throw new JDBCException( "Could not create JDBC Blob", e );
		}
//		catch (IOException e) {
//			throw new HibernateException( "Could not create JDBC Blob", e );
//		}
	}

	/**
	 * Obtain a {@link Clob} instance which can be written to a JDBC
	 * {@link java.sql.PreparedStatement} using
	 * {@link java.sql.PreparedStatement#setClob(int, Clob)}.
	 */
	@Override
	public Clob toJdbcClob(Clob clob) {
		try {
			if ( useConnectionToCreateLob ) {
//				final Clob jdbcClob = createClob();
//				clob.getCharacterStream().transferTo( jdbcClob.setCharacterStream(1) );
//				return jdbcClob;
				return createClob( clob.getSubString( 1, (int) clob.length() ) );
			}
			else {
				return super.toJdbcClob( clob );
			}
		}
		catch (SQLException e) {
			throw new JDBCException( "Could not create JDBC Clob", e );
		}
//		catch (IOException e) {
//			throw new HibernateException( "Could not create JDBC Clob", e );
//		}
	}

	/**
	 * Obtain an {@link NClob} instance which can be written to a JDBC
	 * {@link java.sql.PreparedStatement} using
	 * {@link java.sql.PreparedStatement#setNClob(int, NClob)}.
	 */
	@Override
	public NClob toJdbcNClob(NClob clob) {
		try {
			if ( useConnectionToCreateLob ) {
//				final NClob jdbcClob = createNClob();
//				clob.getCharacterStream().transferTo( jdbcClob.setCharacterStream(1) );
//				return jdbcClob;
				return createNClob( clob.getSubString( 1, (int) clob.length() ) );
			}
			else {
				return super.toJdbcNClob( clob );
			}
		}
		catch (SQLException e) {
			throw new JDBCException( "Could not create JDBC NClob", e );
		}
//		catch (IOException e) {
//			throw new HibernateException( "Could not create JDBC NClob", e );
//		}
	}
}
