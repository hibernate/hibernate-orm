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
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

/**
 * {@link LobCreator} implementation using non-contextual or local creation, meaning that we generate the LOB
 * references ourselves as opposed to delegating to the JDBC {@link java.sql.Connection}.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class NonContextualLobCreator extends AbstractLobCreator implements LobCreator {
	/**
	 * Singleton access
	 */
	public static final NonContextualLobCreator INSTANCE = new NonContextualLobCreator();

	private NonContextualLobCreator() {
	}

	@Override
	public Blob createBlob(byte[] bytes) {
		return BlobProxy.generateProxy( bytes );
	}

	@Override
	public Blob createBlob(InputStream stream, long length) {
		return BlobProxy.generateProxy( stream, length );
	}

	@Override
	public Clob createClob(String string) {
		return ClobProxy.generateProxy( string );
	}

	@Override
	public Clob createClob(Reader reader, long length) {
		return ClobProxy.generateProxy( reader, length );
	}

	@Override
	public NClob createNClob(String string) {
		return NClobProxy.generateProxy( string );
	}

	@Override
	public NClob createNClob(Reader reader, long length) {
		return NClobProxy.generateProxy( reader, length );
	}
}
