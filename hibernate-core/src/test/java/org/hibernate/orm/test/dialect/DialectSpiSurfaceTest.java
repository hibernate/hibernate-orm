/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.LobMergeStrategy;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.TrimSpec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Verifies the ready declaration-level portion of the Dialect SPI surface.
///
/// @author Steve Ebersole
public class DialectSpiSurfaceTest {
	@Test
	void focusedRenderingHooks() throws NoSuchMethodException {
		assertRoles( Dialect.class.getDeclaredMethod( "castPattern", CastType.class, CastType.class ), USE, IMPLEMENT );
		assertRoles( Dialect.class.getDeclaredMethod( "trimPattern", TrimSpec.class, boolean.class ), USE, IMPLEMENT );
		assertRoles(
				Dialect.class.getDeclaredMethod( "getSetOperatorSqlString", SetOperator.class ),
				USE,
				IMPLEMENT
		);
	}

	@Test
	void stockStrategiesAndSupplyPoints() throws NoSuchFieldException, NoSuchMethodException {
		assertRoles( Dialect.class.getDeclaredField( "LEGACY_LOB_MERGE_STRATEGY" ), USE );
		assertRoles( Dialect.class.getDeclaredField( "STREAM_XFER_LOB_MERGE_STRATEGY" ), USE );
		assertRoles( Dialect.class.getDeclaredField( "NEW_LOCATOR_LOB_MERGE_STRATEGY" ), USE );
		assertRoles( Dialect.class.getDeclaredField( "STANDARD_MULTI_KEY_LOAD_SIZING_STRATEGY" ), USE );

		assertRoles( Dialect.class.getDeclaredMethod( "getLobMergeStrategy" ), IMPLEMENT, SUPPLY );
		assertRoles( Dialect.class.getDeclaredMethod( "getMultiKeyLoadSizingStrategy" ), IMPLEMENT, SUPPLY );
		assertRoles( Dialect.class.getDeclaredMethod( "getBatchLoadSizingStrategy" ), IMPLEMENT, SUPPLY );
	}

	@Test
	void suppliedStrategyContracts() {
		assertThat( LobMergeStrategy.class.getAnnotation( SPI.class ).value() ).containsExactly( IMPLEMENT );
		assertThat( MultiKeyLoadSizingStrategy.class.getAnnotation( SPI.class ).value() ).containsExactly( IMPLEMENT );
	}

	private static void assertRoles(Method method, SPI.Role... roles) {
		assertThat( method.getAnnotation( SPI.class ).value() ).containsExactly( roles );
	}

	private static void assertRoles(Field field, SPI.Role... roles) {
		assertThat( field.getAnnotation( SPI.class ).value() ).containsExactly( roles );
	}
}
