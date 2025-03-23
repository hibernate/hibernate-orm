/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.BlobImplementer;
import org.hibernate.engine.jdbc.ClobImplementer;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.env.internal.NonContextualLobCreator;
import org.hibernate.engine.jdbc.proxy.WrappedBlob;
import org.hibernate.engine.jdbc.proxy.WrappedClob;
import org.hibernate.engine.jdbc.env.internal.BlobAndClobCreator;
import org.hibernate.engine.jdbc.env.internal.LobCreationHelper;
import org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl;
import org.hibernate.engine.jdbc.env.internal.LobTypes;
import org.hibernate.engine.jdbc.env.internal.StandardLobCreator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class LobCreatorTest {

	@Test
	public void testConnectedLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl( LobTypes.BLOB, LobTypes.CLOB, LobTypes.NCLOB ) );
		final H2Dialect dialect = new H2Dialect();

		final EnumSet<LobTypes> supportedContextualLobTypes = LobCreationHelper.getSupportedContextualLobTypes(
				dialect,
				Collections.emptyMap(),
				connection
		);

		final LobCreatorBuilderImpl creatorBuilder = new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(), supportedContextualLobTypes );
		final LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		final LobCreator lobCreator = creatorBuilder.buildLobCreator( lobCreationContext );
		assertThat( lobCreator ).isInstanceOf( StandardLobCreator.class );

		testLobCreation( lobCreator );

		connection.close();
	}

	@Test
	public void testJdbc3LobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 3, new JdbcLobBuilderImpl() );
		final H2Dialect dialect = new H2Dialect();

		final EnumSet<LobTypes> supportedContextualLobTypes = LobCreationHelper.getSupportedContextualLobTypes(
				dialect,
				Collections.emptyMap(),
				connection
		);

		final LobCreatorBuilderImpl creatorBuilder = new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(), supportedContextualLobTypes );
		final LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		final LobCreator lobCreator = creatorBuilder.buildLobCreator( lobCreationContext );
		assertThat( lobCreator ).isSameAs( NonContextualLobCreator.INSTANCE );

		testLobCreation( lobCreator );

		connection.close();
	}
	@Test
	public void testJdbc4UnsupportedLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl() );
		final H2Dialect dialect = new H2Dialect();

		final EnumSet<LobTypes> supportedContextualLobTypes = LobCreationHelper.getSupportedContextualLobTypes(
				dialect,
				Collections.emptyMap(),
				connection
		);

		final LobCreatorBuilderImpl creatorBuilder = new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(), supportedContextualLobTypes );
		final LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		final LobCreator lobCreator = creatorBuilder.buildLobCreator( lobCreationContext );
		assertThat( lobCreator ).isSameAs( NonContextualLobCreator.INSTANCE );

		testLobCreation( lobCreator );

		connection.close();
	}
	@Test
	public void testConfiguredNonContextualLobCreator() throws SQLException {
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl( LobTypes.BLOB, LobTypes.CLOB, LobTypes.NCLOB ) );
		final H2Dialect dialect = new H2Dialect();
		final Map<String,Object> props = new HashMap<>();
		props.put( Environment.NON_CONTEXTUAL_LOB_CREATION, "true" );

		final EnumSet<LobTypes> supportedContextualLobTypes = LobCreationHelper.getSupportedContextualLobTypes(
				dialect,
				props,
				connection
		);
		final LobCreatorBuilderImpl creatorBuilder = new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(), supportedContextualLobTypes );
		final LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		final LobCreator lobCreator = creatorBuilder.buildLobCreator( lobCreationContext );
		assertThat( lobCreator ).isSameAs( NonContextualLobCreator.INSTANCE );

		testLobCreation( lobCreator );
		connection.close();
	}

	@Test
	public void testBlobAndClob() throws SQLException {
		// no NCLOB
		final Connection connection = createConnectionProxy( 4, new JdbcLobBuilderImpl( LobTypes.BLOB, LobTypes.CLOB ) );
		final SybaseDialect dialect = new SybaseDialect();
		final EnumSet<LobTypes> supportedContextualLobTypes = LobCreationHelper.getSupportedContextualLobTypes(
				dialect,
				Collections.emptyMap(),
				connection
		);
		final LobCreatorBuilderImpl creatorBuilder = new LobCreatorBuilderImpl( dialect.useConnectionToCreateLob(), supportedContextualLobTypes );
		final LobCreationContext lobCreationContext = new LobCreationContextImpl( connection );

		final LobCreator lobCreator = creatorBuilder.buildLobCreator( lobCreationContext );
		assertThat( lobCreator ).isInstanceOf( BlobAndClobCreator.class );

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
			assertTrue( nclob instanceof NClob );
		}
//		assertTrue( nclob instanceof NClob );
		nclob = lobCreator.wrap( nclob );
		assertTrue( nclob instanceof WrappedClob );

		blob.free();
		clob.free();
		nclob.free();
	}

	private static class LobCreationContextImpl implements LobCreationContext {
		private final Connection connection;

		private LobCreationContextImpl(Connection connection) {
			this.connection = connection;
		}

		public <T> T execute(Callback<T> callback) {
			try {
				return callback.executeOnConnection( connection );
			}
			catch ( SQLException e ) {
				throw new RuntimeException( "Unexpected SQLException", e );
			}
		}
	}

	private interface JdbcLobBuilder {
		Blob createBlob() throws SQLException ;
		Clob createClob() throws SQLException ;
		NClob createNClob() throws SQLException ;
	}

	private static class JdbcLobBuilderImpl implements JdbcLobBuilder {
		private final Set<LobTypes> supportedTypes;

		private JdbcLobBuilderImpl(LobTypes... supportedTypes) {
			this.supportedTypes = convert( supportedTypes );
		}

		private static Set<LobTypes> convert(LobTypes... supportedTypes) {
			final Set<LobTypes> result = new HashSet<>();
			result.addAll( Arrays.asList( supportedTypes ) );
			return result;
		}

		public Blob createBlob() throws SQLException {
			if ( ! supportedTypes.contains( LobTypes.BLOB ) ) {
				throw new SQLException( "not supported!" );
			}
			return new JdbcBlob();
		}

		public Clob createClob() throws SQLException  {
			if ( ! supportedTypes.contains( LobTypes.CLOB ) ) {
				throw new SQLException( "not supported!" );
			}
			return new JdbcClob();
		}

		public NClob createNClob() throws SQLException  {
			if ( ! supportedTypes.contains( LobTypes.NCLOB ) ) {
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
				switch (methodName) {
					case "createBlob":
						return lobBuilder.createBlob();
					case "createClob":
						return lobBuilder.createClob();
					case "createNClob":
						return lobBuilder.createNClob();
					case "getMetaData":
						return metadata;
				}
			}
			return null;
		}
	}

	private static final Class<?>[] CONN_PROXY_TYPES = new Class[] { Connection.class };

	private Connection createConnectionProxy(int version, JdbcLobBuilder jdbcLobBuilder) {
		ConnectionProxyHandler handler = new ConnectionProxyHandler( version, jdbcLobBuilder );
		return ( Connection ) Proxy.newProxyInstance( getClass().getClassLoader(), CONN_PROXY_TYPES, handler );
	}

	private static class MetadataProxyHandler implements InvocationHandler {
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

	private static final Class<?>[] META_PROXY_TYPES = new Class[] { DatabaseMetaData.class };

	private DatabaseMetaData createMetadataProxy(int  version) {
		MetadataProxyHandler handler = new MetadataProxyHandler( version );
		return ( DatabaseMetaData ) Proxy.newProxyInstance( getClass().getClassLoader(), META_PROXY_TYPES, handler );
	}

	private static class JdbcBlob implements Blob {
		public long length() throws SQLException {
			return 0;
		}

		public byte[] getBytes(long pos, int length) throws SQLException {
			return new byte[0];
		}

		public InputStream getBinaryStream() {
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

		public OutputStream setBinaryStream(long pos) {
			return null;
		}

		public void truncate(long len) {
		}

		public void free() throws SQLException {
		}

		public InputStream getBinaryStream(long pos, long length) {
			return null;
		}
	}

	private static class JdbcClob implements Clob {
		public long length() throws SQLException {
			return 0;
		}

		public String getSubString(long pos, int length) {
			return null;
		}

		public Reader getCharacterStream() {
			return null;
		}

		public InputStream getAsciiStream() {
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

		public OutputStream setAsciiStream(long pos) {
			return null;
		}

		public Writer setCharacterStream(long pos) {
			return null;
		}

		public void truncate(long len) {
		}

		public void free() throws SQLException {
		}

		public Reader getCharacterStream(long pos, long length) {
			return null;
		}
	}

	private static class JdbcNClob extends JdbcClob implements NClob {
	}
}
