/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hibernate.SPI;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.MySQLServerConfiguration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgreSQLDriverKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Verifies the supported Dialect and family-base construction surface.
///
/// @author Steve Ebersole
public class DialectSpiConstructionTest {
	@Test
	void dialectConstructionAndIdentityContract() throws NoSuchMethodException {
		assertImplementConstructor( Dialect.class, DatabaseVersion.class );
		assertImplementConstructor( Dialect.class, DialectResolutionInfo.class );

		final Method determineDatabaseVersion =
				Dialect.class.getDeclaredMethod( "determineDatabaseVersion", DialectResolutionInfo.class );
		assertThat( Modifier.isPublic( determineDatabaseVersion.getModifiers() ) ).isTrue();
		assertThat( determineDatabaseVersion.getAnnotation( SPI.class ).value() )
				.containsExactly( USE, IMPLEMENT );

		final Method getVersion = Dialect.class.getDeclaredMethod( "getVersion" );
		assertThat( Modifier.isFinal( getVersion.getModifiers() ) ).isTrue();
		assertThat( getVersion.getAnnotation( SPI.class ).value() ).containsExactly( USE );

		final Method getMinimumSupportedVersion =
				Dialect.class.getDeclaredMethod( "getMinimumSupportedVersion" );
		assertThat( Modifier.isProtected( getMinimumSupportedVersion.getModifiers() ) ).isTrue();
		assertThat( getMinimumSupportedVersion.getAnnotation( SPI.class ).value() )
				.containsExactly( IMPLEMENT, SUPPLY );

		assertThat( Modifier.isPrivate( Dialect.class.getDeclaredMethod( "checkVersion" ).getModifiers() ) ).isTrue();
		assertThat( Modifier.isFinal( Dialect.class.getDeclaredMethod( "toString" ).getModifiers() ) ).isTrue();
	}

	@Test
	void postgreSqlFamilyConstructionContract() throws NoSuchMethodException {
		assertImplementConstructor( PostgreSQLDialect.class );
		assertImplementConstructor( PostgreSQLDialect.class, DialectResolutionInfo.class );
		assertImplementConstructor( PostgreSQLDialect.class, DatabaseVersion.class );
		assertImplementConstructor(
				PostgreSQLDialect.class,
				DatabaseVersion.class,
				PostgreSQLDriverKind.class
		);
		assertUseSpi( PostgreSQLDriverKind.class );
	}

	@Test
	void mySqlFamilyConstructionContract() throws NoSuchMethodException {
		assertImplementConstructor( MySQLDialect.class );
		assertImplementConstructor( MySQLDialect.class, DialectResolutionInfo.class );
		assertImplementConstructor( MySQLDialect.class, DatabaseVersion.class );
		assertImplementConstructor(
				MySQLDialect.class,
				DatabaseVersion.class,
				MySQLServerConfiguration.class
		);
		assertUseSpi( MySQLServerConfiguration.class );

		assertThatExceptionOfType( NoSuchMethodException.class )
				.isThrownBy( () -> MySQLDialect.class.getDeclaredConstructor( DatabaseVersion.class, int.class ) );
		assertThatExceptionOfType( NoSuchMethodException.class )
				.isThrownBy( () -> MySQLDialect.class.getDeclaredConstructor(
						DatabaseVersion.class,
						int.class,
						boolean.class
				) );
	}

	@Test
	void db2AndTransactSqlFamilyConstructionContract() throws NoSuchMethodException {
		assertImplementConstructor( DB2Dialect.class );
		assertImplementConstructor( DB2Dialect.class, DialectResolutionInfo.class );
		assertImplementConstructor( DB2Dialect.class, DatabaseVersion.class );

		assertProtectedImplementConstructor( AbstractTransactSQLDialect.class, DatabaseVersion.class );
		assertProtectedImplementConstructor( AbstractTransactSQLDialect.class, DialectResolutionInfo.class );
	}

	private static void assertUseSpi(Class<?> type) {
		assertThat( type.getAnnotation( SPI.class ).value() ).containsExactly( USE );
	}

	private static void assertImplementConstructor(Class<?> type, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		final Constructor<?> constructor = type.getDeclaredConstructor( parameterTypes );
		assertThat( constructor.getAnnotation( SPI.class ).value() ).containsExactly( IMPLEMENT );
	}

	private static void assertProtectedImplementConstructor(Class<?> type, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		final Constructor<?> constructor = type.getDeclaredConstructor( parameterTypes );
		assertThat( Modifier.isProtected( constructor.getModifiers() ) ).isTrue();
		assertThat( constructor.getAnnotation( SPI.class ).value() ).containsExactly( IMPLEMENT );
	}
}
