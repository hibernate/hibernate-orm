/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.AbstractLobCreator;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NClobProxy;
import org.hibernate.engine.jdbc.NonContextualLobCreator;

/**
 * LobCreator which can use {@link Connection#createBlob} and {@link Connection#createClob},
 * but {@link java.sql.NClob} references are created locally.
 *
 * @see NClobProxy
 *
 * @author Steve Ebersole
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

	protected final LobCreationContext lobCreationContext;

	public BlobAndClobCreator(LobCreationContext lobCreationContext) {
		this.lobCreationContext = lobCreationContext;
	}

	/**
	 * Create the basic contextual BLOB reference.
	 *
	 * @return The created BLOB reference.
	 */
	public Blob createBlob() {
		return lobCreationContext.fromContext( CREATE_BLOB_CALLBACK );
	}

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

	@Override
	public Blob createBlob(InputStream stream, long length) {
		// IMPL NOTE : it is inefficient to use JDBC LOB locator creation to create a LOB
		// backed by a given stream.  So just wrap the stream (which is what the NonContextualLobCreator does).
		return NonContextualLobCreator.INSTANCE.createBlob( stream, length );
	}

	/**
	 * Create the basic contextual CLOB reference.
	 *
	 * @return The created CLOB reference.
	 */
	public Clob createClob() {
		return lobCreationContext.fromContext( CREATE_CLOB_CALLBACK );
	}

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

	@Override
	public Clob createClob(Reader reader, long length) {
		// IMPL NOTE : it is inefficient to use JDBC LOB locator creation to create a LOB
		// backed by a given stream.  So just wrap the stream (which is what the NonContextualLobCreator does).
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
}
