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
package org.hibernate.engine.jdbc.jdbc3;

import java.sql.SQLException;
import java.sql.Blob;
import java.sql.Clob;

import junit.framework.TestCase;

import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.JdbcSupportLoader;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.WrappedBlob;
import org.hibernate.engine.jdbc.WrappedClob;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class JdbcSupportTest extends TestCase {
	public void testLobCreator() throws ClassNotFoundException, SQLException {
		final LobCreationContext lobCreationContext = new LobCreationContext() {
			public Object execute(Callback callback) {
				fail( "Unexpeted call to getConnection" );
				return null;
			}
		};

		LobCreator lobCreator = JdbcSupportLoader.loadJdbcSupport( null ).getLobCreator( lobCreationContext );

		Blob blob = lobCreator.createBlob( new byte[] {} );
		assertTrue( blob instanceof BlobImplementer );
		blob = lobCreator.wrap( blob );
		assertTrue( blob instanceof WrappedBlob );

		Clob clob = lobCreator.createClob( "Hi" );
		assertTrue( clob instanceof ClobImplementer );
		clob = lobCreator.wrap( clob );
		assertTrue( clob instanceof WrappedClob );

		Clob nclob = lobCreator.createNClob( "Hi" );
		assertTrue( nclob instanceof NClobImplementer );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

	}
}
