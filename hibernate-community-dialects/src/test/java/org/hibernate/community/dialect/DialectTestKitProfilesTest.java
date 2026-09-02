/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.testing.DialectTestKit;
import org.hibernate.dialect.testing.spi.DialectContractProfile;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

/// Exercises representative community Dialects as consumers of the published
/// provider test kit.
///
/// @author Steve Ebersole
class DialectTestKitProfilesTest {
	@TestFactory
	List<DynamicContainer> communityDialectContracts() {
		return List.of(
				contracts( "Informix", InformixDialect::new ),
				contracts( "Sybase Legacy", SybaseLegacyDialect::new ),
				contracts( "SQLite", SQLiteDialect::new )
		);
	}

	private static DynamicContainer contracts(String name, DialectFactory factory) {
		final DatabaseVersion expectedVersion = factory.create().getVersion();
		return DialectTestKit.contractTests( new DialectContractProfile() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public Dialect createDialect() {
				return factory.create();
			}

			@Override
			public DatabaseVersion expectedDatabaseVersion() {
				return expectedVersion;
			}
		} );
	}

	@FunctionalInterface
	private interface DialectFactory {
		Dialect create();
	}
}
