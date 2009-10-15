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
import java.sql.Clob;
import java.io.InputStream;
import java.io.Reader;

/**
 * {@link LobCreator} implementation using non-contextual or local creation, meaning that we generate the LOB
 * references ourselves as opposed to delegating to the JDBC {@link java.sql.Connection}.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class NonContextualLobCreator extends AbstractLobCreator implements LobCreator {
	public static final NonContextualLobCreator INSTANCE = new NonContextualLobCreator();

	private NonContextualLobCreator() {
	}

	/**
	 * {@inheritDoc}
	 */
	public Blob createBlob(byte[] bytes) {
		return BlobProxy.generateProxy( bytes );
	}

	/**
	 * {@inheritDoc}
	 */
	public Blob createBlob(InputStream stream, long length) {
		return BlobProxy.generateProxy( stream, length );
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createClob(String string) {
		return ClobProxy.generateProxy( string );
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createClob(Reader reader, long length) {
		return ClobProxy.generateProxy( reader, length );
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createNClob(String string) {
		return NClobProxy.generateProxy( string );
	}

	/**
	 * {@inheritDoc}
	 */
	public Clob createNClob(Reader reader, long length) {
		return NClobProxy.generateProxy( reader, length );
	}
}
