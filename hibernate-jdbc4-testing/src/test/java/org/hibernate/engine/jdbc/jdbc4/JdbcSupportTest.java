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
package org.hibernate.engine.jdbc.jdbc4;

import java.sql.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import junit.framework.TestCase;

import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.JdbcSupportLoader;
import org.hibernate.engine.jdbc.ContextualLobCreator;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.WrappedBlob;
import org.hibernate.engine.jdbc.WrappedClob;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class JdbcSupportTest extends TestCase {
	public void testConnectedLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy(
				4,
				new JdbcLobBuilder() {
					public Blob createBlob() {
						return new JdbcBlob();
					}

					public Clob createClob() {
						return new JdbcClob();
					}

					public NClob createNClob() {
						return new JdbcNClob();
					}
				}
		);
		final LobCreationContext lobCreationContext = new LobCreationContext() {
			public Object execute(Callback callback) {
				try {
					return callback.executeOnConnection( connection );
				}
				catch ( SQLException e ) {
					throw new RuntimeException( "Unexpected SQLException", e );
				}
			}
		};

		LobCreator lobCreator = JdbcSupportLoader.loadJdbcSupport( connection ).getLobCreator( lobCreationContext );
		assertTrue( lobCreator instanceof ContextualLobCreator );

		Blob blob = lobCreator.createBlob( new byte[] {} );
		assertTrue( blob instanceof JdbcBlob );
		blob = lobCreator.wrap( blob );
		assertTrue( blob instanceof WrappedBlob );

		Clob clob = lobCreator.createClob( "Hi" );
		assertTrue( clob instanceof JdbcClob );
		clob = lobCreator.wrap( clob );
		assertTrue( clob instanceof WrappedClob );

		Clob nclob = lobCreator.createNClob( "Hi" );
		assertTrue( nclob instanceof JdbcNClob );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
		connection.close();
	}

	public void testConnectedLobCreatorWithUnSupportedCreations() throws SQLException {
		final Connection connection = createConnectionProxy(
				3,
				new JdbcLobBuilder() {
					public Blob createBlob() {
						throw new UnsupportedOperationException();
					}

					public Clob createClob() {
						throw new UnsupportedOperationException();
					}

					public NClob createNClob() {
						throw new UnsupportedOperationException();
					}
				}
		);
		final LobCreationContext lobCreationContext = new LobCreationContext() {
			public Object execute(Callback callback) {
				try {
					return callback.executeOnConnection( connection );
				}
				catch ( SQLException e ) {
					throw new RuntimeException( "Unexpected SQLException", e );
				}
			}
		};

		LobCreator lobCreator = JdbcSupportLoader.loadJdbcSupport( connection ).getLobCreator( lobCreationContext );
		assertTrue( lobCreator instanceof NonContextualLobCreator );

		Blob blob = lobCreator.createBlob( new byte[] {} );
		assertTrue( blob instanceof BlobImplementer );
		blob = lobCreator.wrap( blob );
		assertTrue( blob instanceof WrappedBlob );

		Clob clob = lobCreator.createClob( "Hi" );
		assertTrue( clob instanceof ClobImplementer );
		clob = lobCreator.wrap( clob );
		assertTrue( clob instanceof WrappedClob );

		Clob nclob = lobCreator.createNClob( "Hi" );
		assertTrue( nclob instanceof ClobImplementer );
		assertTrue( nclob instanceof NClobImplementer );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
		connection.close();
	}

	public void testLegacyLobCreator() throws SQLException {
		LobCreator lobCreator = JdbcSupportLoader.loadJdbcSupport( null ).getLobCreator();

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
		assertTrue( NClob.class.isInstance( nclob ) );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
	}

	private interface JdbcLobBuilder {
		public Blob createBlob();
		public Clob createClob();
		public NClob createNClob();
	}

	private class ConnectionProxyHandler implements InvocationHandler {
		private final JdbcLobBuilder lobBuilder;
		private final DatabaseMetaData metadata;

		private ConnectionProxyHandler(int version, JdbcLobBuilder lobBuilder) {
			this.lobBuilder = lobBuilder;
			this.metadata = createMetadataProxy( version );
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// the only methods we are interested in are the LOB creation methods...
			if ( args == null || args.length == 0 ) {
				final String methodName = method.getName();
				if ( "createBlob".equals( methodName ) ) {
					return lobBuilder.createBlob();
				}
				else if ( "createClob".equals( methodName ) ) {
					return lobBuilder.createClob();
				}
				else if ( "createNClob".equals( methodName ) ) {
					return lobBuilder.createNClob();
				}
				else if ( "getMetaData".equals( methodName ) ) {
					return metadata;
				}
			}
			return null;
		}
	}

	private static Class[] CONN_PROXY_TYPES = new Class[] { Connection.class };

	private Connection createConnectionProxy(int version, JdbcLobBuilder jdbcLobBuilder) {
		ConnectionProxyHandler handler = new ConnectionProxyHandler( version, jdbcLobBuilder );
		return ( Connection ) Proxy.newProxyInstance( getClass().getClassLoader(), CONN_PROXY_TYPES, handler );
	}

	private class MetadataProxyHandler implements InvocationHandler {
		private final int jdbcVersion;

		private MetadataProxyHandler(int jdbcVersion) {
			this.jdbcVersion = jdbcVersion;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "getJDBCMajorVersion".equals( methodName ) ) {
				return jdbcVersion;
			}
			return null;
		}
	}

	private static Class[] META_PROXY_TYPES = new Class[] { DatabaseMetaData.class };

	private DatabaseMetaData createMetadataProxy(int  version) {
		MetadataProxyHandler handler = new MetadataProxyHandler( version );
		return ( DatabaseMetaData ) Proxy.newProxyInstance( getClass().getClassLoader(), META_PROXY_TYPES, handler );
	}

	private class JdbcBlob implements Blob {
		public long length() throws SQLException {
			return 0;
		}

		public byte[] getBytes(long pos, int length) throws SQLException {
			return new byte[0];
		}

		public InputStream getBinaryStream() throws SQLException {
			return null;
		}

		public long position(byte[] pattern, long start) throws SQLException {
			return 0;
		}

		public long position(Blob pattern, long start) throws SQLException {
			return 0;
		}

		public int setBytes(long pos, byte[] bytes) throws SQLException {
			return 0;
		}

		public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
			return 0;
		}

		public OutputStream setBinaryStream(long pos) throws SQLException {
			return null;
		}

		public void truncate(long len) throws SQLException {
		}

		public void free() throws SQLException {
		}

		public InputStream getBinaryStream(long pos, long length) throws SQLException {
			return null;
		}
	}

	private class JdbcClob implements Clob {
		public long length() throws SQLException {
			return 0;
		}

		public String getSubString(long pos, int length) throws SQLException {
			return null;
		}

		public Reader getCharacterStream() throws SQLException {
			return null;
		}

		public InputStream getAsciiStream() throws SQLException {
			return null;
		}

		public long position(String searchstr, long start) throws SQLException {
			return 0;
		}

		public long position(Clob searchstr, long start) throws SQLException {
			return 0;
		}

		public int setString(long pos, String str) throws SQLException {
			return 0;
		}

		public int setString(long pos, String str, int offset, int len) throws SQLException {
			return 0;
		}

		public OutputStream setAsciiStream(long pos) throws SQLException {
			return null;
		}

		public Writer setCharacterStream(long pos) throws SQLException {
			return null;
		}

		public void truncate(long len) throws SQLException {
		}

		public void free() throws SQLException {
		}

		public Reader getCharacterStream(long pos, long length) throws SQLException {
			return null;
		}
	}

	private class JdbcNClob extends JdbcClob implements NClob {
	}
}
