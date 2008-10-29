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

import java.sql.Blob;
import java.sql.Clob;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;

/**
 * A LobCreator implementation that can be used with JVMs and JDBC drivers that support JDBC3.
 *
 * @author Gail Badner
 */
public class LobCreatorImplJDBC3 implements LobCreator {

	/* package */
	LobCreatorImplJDBC3() {
	}

	/**
 	 * {@inheritDoc}
	 */
	public Blob createBlob(byte[] bytes) {
		return SerializableBlobProxy.generateProxy( BlobImplProxy.generateProxy( bytes ) );
	}

	/**
 	 * {@inheritDoc}
	 */
	public Blob createBlob(InputStream is, int length) {
		return SerializableBlobProxy.generateProxy( BlobImplProxy.generateProxy( is, length ) );
	}

	/**
 	 * {@inheritDoc}
	 */
	public Blob createBlob(InputStream is) throws IOException {
		return SerializableBlobProxy.generateProxy( BlobImplProxy.generateProxy( is, is.available() ) );
	}

	/**
 	 * {@inheritDoc}
	 */
	public Clob createClob(String string) {
		return SerializableClobProxy.generateProxy( ClobImplProxy.generateProxy( string ) );
	}

	/**
 	 * {@inheritDoc}
	 */
	public Clob createClob(Reader reader, int length) {
		return SerializableClobProxy.generateProxy( ClobImplProxy.generateProxy( reader, length ) );
	}
}
