/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.ContextualLobCreator;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.WrappedBlob;
import org.hibernate.engine.jdbc.WrappedClob;
import org.hibernate.engine.jdbc.internal.LobCreatorBuilder;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class LobCreatorTest extends org.hibernate.testing.junit4.BaseUnitTestCase {

	@Test
	public void testConnectedLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl( true ) );
		LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		LobCreator lobCreator =
				new LobCreatorBuilder( new Properties(), connection )
						.buildLobCreator( lobCreationContext );
		assertTrue( lobCreator instanceof ContextualLobCreator );
		testLobCreation( lobCreator );

		connection.close();
	}
    @Test
	public void testJdbc3LobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 3, new JdbcLobBuilderImpl( false) );
		LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		LobCreator lobCreator =
				new LobCreatorBuilder( new Properties(), connection )
						.buildLobCreator( lobCreationContext );
		assertSame( NonContextualLobCreator.INSTANCE, lobCreator );

		testLobCreation( lobCreator );
		connection.close();
	}
    @Test
	public void testJdbc4UnsupportedLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl( false ) );
		LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		LobCreator lobCreator =
				new LobCreatorBuilder( new Properties(), connection )
						.buildLobCreator( lobCreationContext );
		assertSame( NonContextualLobCreator.INSTANCE, lobCreator );

		testLobCreation( lobCreator );
		connection.close();
	}
    @Test
	public void testConfiguredNonContextualLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl( true ) );
		LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		Properties props = new Properties();
		props.setProperty( Environment.NON_CONTEXTUAL_LOB_CREATION, "true" );
		LobCreator lobCreator =
				new LobCreatorBuilder( props, connection )
						.buildLobCreator( lobCreationContext );
		assertSame( NonContextualLobCreator.INSTANCE, lobCreator );

		testLobCreation( lobCreator );
		connection.close();
	}

	private void testLobCreation(LobCreator lobCreator) throws SQLException{
		Blob blob = lobCreator.createBlob( new byte[] {} );
		if ( lobCreator == NonContextualLobCreator.INSTANCE ) {
			assertTrue( blob instanceof BlobImplementer );
		}
		else {
			assertTrue( blob instanceof JdbcBlob );
		}
		blob = lobCreator.wrap( blob );
		assertTrue( blob instanceof WrappedBlob );

		Clob clob = lobCreator.createClob( "Hi" );
		if ( lobCreator == NonContextualLobCreator.INSTANCE ) {
			assertTrue( clob instanceof ClobImplementer );
		}
		else {
			assertTrue( clob instanceof JdbcClob );
		}
		clob = lobCreator.wrap( clob );
		assertTrue( clob instanceof WrappedClob );

		Clob nclob = lobCreator.createNClob( "Hi" );
		if ( lobCreator == NonContextualLobCreator.INSTANCE ) {
			assertTrue( nclob instanceof NClobImplementer );
		}
		else {
			assertTrue( nclob instanceof JdbcNClob );
		}
		assertTrue( NClob.class.isInstance( nclob ) );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
	}

	private class LobCreationContextImpl implements LobCreationContext {
		private Connection connection;

		private LobCreationContextImpl(Connection connection) {
			this.connection = connection;
		}

		public <T> T execute(LobCreationContext.Callback<T> callback) {
			try {
				return callback.executeOnConnection( connection );
			}
			catch ( SQLException e ) {
				throw new RuntimeException( "Unexpected SQLException", e );
			}
		}
	}

	private interface JdbcLobBuilder {
		public Blob createBlob() throws SQLException ;
		public Clob createClob() throws SQLException ;
		public NClob createNClob() throws SQLException ;
	}

	private class JdbcLobBuilderImpl implements JdbcLobBuilder {
		private final boolean isSupported;

		private JdbcLobBuilderImpl(boolean isSupported) {
			this.isSupported = isSupported;
		}
		public Blob createBlob() throws SQLException {
			if ( ! isSupported ) {
				throw new SQLException( "not supported!" );
			}
			return new JdbcBlob();
		}

		public Clob createClob() throws SQLException  {
			if ( ! isSupported ) {
				throw new SQLException( "not supported!" );
			}
			return new JdbcClob();
		}

		public NClob createNClob() throws SQLException  {
			if ( ! isSupported ) {
				throw new SQLException( "not supported!" );
			}
			return new JdbcNClob();
		}
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
