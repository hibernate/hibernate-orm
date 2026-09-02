/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.JDBCException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises effective JDBC metadata and custom REF_CURSOR access from the
/// standalone external Dialect fixture.
///
/// @author Steve Ebersole
public class ExampleJdbcMetadataAndRefCursorTest {
	@Test
	void effectiveNoMetadataProfileIsAvailableThroughTheCanonicalAccessor() {
		final var registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, ExampleDialect.class.getName() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.build();
		try {
			final JdbcEnvironment environment = registry.requireService( JdbcEnvironment.class );
			final var metadata = environment.getJdbcMetadata();
			assertSame( metadata, environment.getJdbcMetadata() );
			assertSame( metadata, registry.requireService( JdbcServices.class ).getJdbcMetadata() );
			assertTrue( metadata.supportsNamedParameters() );
			assertFalse( metadata.supportsBatchUpdates() );
			assertFalse( metadata.supportsRefCursors() );
			assertFalse( metadata.getExtractedDatabaseMetaData().supportsNamedParameters() );
			assertTrue( metadata.getExtractedDatabaseMetaData().supportsBatchUpdates() );
			assertFalse( metadata.getExtractedDatabaseMetaData().supportsRefCursors() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void customFactorySupportsPositionNameExtractionAndConversion() throws Exception {
		final ExampleDialect dialect = new ExampleDialect();
		assertSame( dialect.getJdbcMetadataOverrides(), dialect.getJdbcMetadataOverrides() );
		assertSame( dialect.getRefCursorSupportFactory(), dialect.getRefCursorSupportFactory() );
		assertEquals(
				JdbcMetadataOverrides.SupportOverride.SUPPORTED,
				dialect.getJdbcMetadataOverrides().getNamedParameterSupport()
		);
		assertEquals(
				JdbcMetadataOverrides.SupportOverride.UNSUPPORTED,
				dialect.getJdbcMetadataOverrides().getBatchUpdateSupport()
		);
		assertEquals(
				JdbcMetadataOverrides.SupportOverride.UNSUPPORTED,
				dialect.getJdbcMetadataOverrides().getStandardRefCursorSupport()
		);

		final List<List<Object>> calls = new ArrayList<>();
		final ResultSet positional = proxy( ResultSet.class, (method, arguments) -> null );
		final ResultSet named = proxy( ResultSet.class, (method, arguments) -> null );
		final CallableStatement statement = proxy( CallableStatement.class, (method, arguments) -> {
			if ( method.equals( "registerOutParameter" ) ) {
				calls.add( List.of( arguments ) );
				return null;
			}
			if ( method.equals( "getObject" ) ) {
				return arguments[0] instanceof Integer ? positional : named;
			}
			return defaultValue( method );
		} );
		final var support = dialect.getRefCursorSupportFactory().createRefCursorSupport( context( false ) );
		support.registerRefCursorParameter( statement, 3 );
		support.registerRefCursorParameter( statement, "rows" );
		assertEquals( List.of( 3, Types.OTHER ), calls.get( 0 ) );
		assertEquals( List.of( "rows", Types.OTHER ), calls.get( 1 ) );
		assertSame( positional, support.getResultSet( statement, 3 ) );
		assertSame( named, support.getResultSet( statement, "rows" ) );

		final SQLException failure = new SQLException( "controlled" );
		final CallableStatement failing = proxy( CallableStatement.class, (method, arguments) -> {
			if ( method.equals( "registerOutParameter" ) ) {
				throw failure;
			}
			return defaultValue( method );
		} );
		final JDBCException converted = assertThrows(
				JDBCException.class,
				() -> support.registerRefCursorParameter( failing, 4 )
		);
		assertSame( failure, converted.getSQLException() );
		assertTrue( converted.getMessage().contains( "4" ) );
	}

	private static RefCursorSupportCreationContext context(boolean standard) {
		return new RefCursorSupportCreationContext() {
			@Override
			public boolean supportsStandardRefCursors() {
				return standard;
			}

			@Override
			public JDBCException convert(SQLException exception, String message) {
				return new JDBCException( message, exception );
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> contract, Invocation invocation) {
		return (T) Proxy.newProxyInstance(
				contract.getClassLoader(),
				new Class<?>[] { contract },
				(proxy, method, arguments) -> invocation.invoke( method.getName(), arguments )
		);
	}

	private static Object defaultValue(String method) {
		return switch ( method ) {
			case "isWrapperFor" -> false;
			case "unwrap" -> null;
			default -> null;
		};
	}

	@FunctionalInterface
	private interface Invocation {
		Object invoke(String method, Object[] arguments) throws Throwable;
	}
}
