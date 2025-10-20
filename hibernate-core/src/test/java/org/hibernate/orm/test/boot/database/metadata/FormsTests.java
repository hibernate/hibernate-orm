/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.metadata;

import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hibernate.cfg.JdbcSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot.ALLOW;
import static org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot.DISALLOW;
import static org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot.REQUIRE;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.jdbcMetadataAccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
public class FormsTests {
	@Test
	void testConfigForms() {
		check( ALLOW_METADATA_ON_BOOT, ALLOW, ALLOW );
		check( ALLOW_METADATA_ON_BOOT, ALLOW.name(), ALLOW );
		check( ALLOW_METADATA_ON_BOOT, ALLOW.name().toLowerCase( Locale.ROOT ), ALLOW );
		check( ALLOW_METADATA_ON_BOOT, true, ALLOW );
		check( ALLOW_METADATA_ON_BOOT, TRUE, ALLOW );
		check( ALLOW_METADATA_ON_BOOT, "true", ALLOW );

		check( ALLOW_METADATA_ON_BOOT, DISALLOW, DISALLOW );
		check( ALLOW_METADATA_ON_BOOT, DISALLOW.name(), DISALLOW );
		check( ALLOW_METADATA_ON_BOOT, DISALLOW.name().toLowerCase( Locale.ROOT ), DISALLOW );
		check( ALLOW_METADATA_ON_BOOT, false, DISALLOW );
		check( ALLOW_METADATA_ON_BOOT, FALSE, DISALLOW );
		check( ALLOW_METADATA_ON_BOOT, "false", DISALLOW );

		check( ALLOW_METADATA_ON_BOOT, REQUIRE, REQUIRE );
		check( ALLOW_METADATA_ON_BOOT, REQUIRE.name(), REQUIRE );
		check( ALLOW_METADATA_ON_BOOT, REQUIRE.name().toLowerCase( Locale.ROOT ), REQUIRE );

		check( JdbcEnvironmentInitiator.USE_JDBC_METADATA_DEFAULTS, TRUE, ALLOW );
		check( JdbcEnvironmentInitiator.USE_JDBC_METADATA_DEFAULTS, true, ALLOW );

		check( JdbcEnvironmentInitiator.USE_JDBC_METADATA_DEFAULTS, FALSE, DISALLOW );
		check( JdbcEnvironmentInitiator.USE_JDBC_METADATA_DEFAULTS, false, DISALLOW );
	}

	private void check(String configName, Object setting, JdbcMetadataOnBoot expected) {
		assertEquals( expected, jdbcMetadataAccess( Map.of( configName, setting ) ) );
	}
}
