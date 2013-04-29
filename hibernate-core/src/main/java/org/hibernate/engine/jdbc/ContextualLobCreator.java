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

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;

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

	/**
	 * Constructs a ContextualLobCreator
	 *
	 * @param lobCreationContext The context for performing LOB creation
	 */
	public ContextualLobCreator(LobCreationContext lobCreationContext) {
		this.lobCreationContext = lobCreationContext;
	}

	/**
	 * Create the basic contextual BLOB reference.
	 *
	 * @return The created BLOB reference.
	 */
	public Blob createBlob() {
		return lobCreationContext.execute( CREATE_BLOB_CALLBACK );
	}

	@Override
	public Blob createBlob(byte[] bytes) {
		try {
			final Blob blob = createBlob();
			blob.setBytes( 1, bytes );
			return blob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set BLOB bytes after creation", e );
		}
	}

	@Override
	public Blob createBlob(InputStream inputStream, long length) {
		// IMPL NOTE : it is inefficient to use JDBC LOB locator creation to create a LOB
		// backed by a given stream.  So just wrap the stream (which is what the NonContextualLobCreator does).
		return NonContextualLobCreator.INSTANCE.createBlob( inputStream, length );
	}

	/**
	 * Create the basic contextual CLOB reference.
	 *
	 * @return The created CLOB reference.
	 */
	public Clob createClob() {
		return lobCreationContext.execute( CREATE_CLOB_CALLBACK );
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

	/**
	 * Create the basic contextual NCLOB reference.
	 *
	 * @return The created NCLOB reference.
	 */
	public NClob createNClob() {
		return lobCreationContext.execute( CREATE_NCLOB_CALLBACK );
	}

	@Override
	public NClob createNClob(String string) {
		try {
			final NClob nclob = createNClob();
			nclob.setString( 1, string );
			return nclob;
		}
		catch ( SQLException e ) {
			throw new JDBCException( "Unable to set NCLOB string after creation", e );
		}
	}

	@Override
	public NClob createNClob(Reader reader, long length) {
		// IMPL NOTE : it is inefficient to use JDBC LOB locator creation to create a LOB
		// backed by a given stream.  So just wrap the stream (which is what the NonContextualLobCreator does).
		return NonContextualLobCreator.INSTANCE.createNClob( reader, length );
	}

	/**
	 * Callback for performing contextual BLOB creation
	 */
	public static final LobCreationContext.Callback<Blob> CREATE_BLOB_CALLBACK = new LobCreationContext.Callback<Blob>() {
		@Override
		public Blob executeOnConnection(Connection connection) throws SQLException {
			return connection.createBlob();
		}
	};

	/**
	 * Callback for performing contextual CLOB creation
	 */
	public static final LobCreationContext.Callback<Clob> CREATE_CLOB_CALLBACK = new LobCreationContext.Callback<Clob>() {
		@Override
		public Clob executeOnConnection(Connection connection) throws SQLException {
			return connection.createClob();
		}
	};

	/**
	 * Callback for performing contextual NCLOB creation
	 */
	public static final LobCreationContext.Callback<NClob> CREATE_NCLOB_CALLBACK = new LobCreationContext.Callback<NClob>() {
		@Override
		public NClob executeOnConnection(Connection connection) throws SQLException {
			return connection.createNClob();
		}
	};
}
