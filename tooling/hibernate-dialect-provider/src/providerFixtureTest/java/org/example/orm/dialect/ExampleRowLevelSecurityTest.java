/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.AFTER_TABLES;
import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.BEFORE_TABLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies provider implementation and supply of the public row-level-
/// security SPI.
///
/// @author Steve Ebersole
/// @since 8.0
public class ExampleRowLevelSecurityTest {
	@Test
	void suppliesTheProviderOwnedStrategy() {
		assertSame( ExampleRowLevelSecurity.INSTANCE, new ExampleDialect().getRowLevelSecurity() );
	}

	@Test
	void supportsOnlySessionAndReturnsBothDdlPhases() {
		final var support = ExampleRowLevelSecurity.INSTANCE;
		assertTrue( support.supportsRowLevelSecurity() );
		assertTrue( support.supportsTenantIdentifierSource( TenantIdentifierSource.SESSION ) );
		assertFalse( support.supportsTenantIdentifierSource( TenantIdentifierSource.DATABASE_USER ) );

		final var ddl = support.getTenantTableDdl( request() );
		assertEquals( 2, ddl.size() );
		assertSame( BEFORE_TABLES, ddl.get( 0 ).phase() );
		assertSame( AFTER_TABLES, ddl.get( 1 ).phase() );
		assertEquals( List.of( "create tenant context for fixture.orders" ), ddl.get( 0 ).createCommands() );
		assertEquals(
				List.of( "create tenant policy on fixture.orders using tenant_id" ),
				ddl.get( 1 ).createCommands()
		);
	}

	@Test
	void executesThePreparedStatementConnectionOperation() throws Exception {
		final List<Object> calls = new ArrayList<>();
		final PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { PreparedStatement.class },
				(proxy, method, arguments) -> {
					if ( method.getName().equals( "setString" ) ) {
						calls.add( arguments[0] );
						calls.add( arguments[1] );
					}
					else if ( method.getName().equals( "execute" ) ) {
						calls.add( "execute" );
						return true;
					}
					return defaultValue( method.getReturnType() );
				}
		);
		final Connection connection = (Connection) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { Connection.class },
				(proxy, method, arguments) -> {
					if ( method.getName().equals( "prepareStatement" ) ) {
						calls.add( arguments[0] );
						return statement;
					}
					return defaultValue( method.getReturnType() );
				}
		);

		ExampleRowLevelSecurity.INSTANCE.setTenantIdentifier( connection, "acme", true );
		assertEquals(
				List.of( "set local example.tenant_identifier = ?", 1, "acme:true", "execute" ),
				calls
		);
	}

	private static RowLevelSecurityDdlRequest request() {
		return new RowLevelSecurityDdlRequest() {
			@Override
			public TenantIdentifierSource tenantIdentifierSource() {
				return TenantIdentifierSource.SESSION;
			}

			@Override
			public String qualifiedTableName() {
				return "fixture.orders";
			}

			@Override
			public String qualifiedTableName(String defaultSchema) {
				return qualifiedTableName();
			}

			@Override
			public String qualifySiblingObject(String objectName, String defaultSchema) {
				return "fixture." + objectName;
			}

			@Override
			public String tableExportIdentifier() {
				return "fixture.orders";
			}

			@Override
			public String tenantColumnName() {
				return "tenant_id";
			}

			@Override
			public String tenantColumnSqlType() {
				return "varchar(64)";
			}

			@Override
			public int tenantColumnSqlTypeCode() {
				return SqlTypes.VARCHAR;
			}
		};
	}

	private static Object defaultValue(Class<?> type) {
		if ( !type.isPrimitive() ) {
			return null;
		}
		if ( type == boolean.class ) {
			return false;
		}
		if ( type == char.class ) {
			return '\0';
		}
		return 0;
	}
}
